package com.joaopaulo.musicas.services;

import com.joaopaulo.musicas.entities.UserSong;
import com.joaopaulo.musicas.exceptions.IdorSecurityException;
import com.joaopaulo.musicas.exceptions.UsuarioNaoEncontradoException;
import com.joaopaulo.musicas.repositories.UserSongRepository;
import com.joaopaulo.musicas.repositories.UsuarioRepository;
import com.joaopaulo.musicas.security.UsuarioDetails;
import com.joaopaulo.musicas.dtos.response.UserSongResponse;
import com.joaopaulo.musicas.enums.MusicSource;
import com.joaopaulo.musicas.mappers.MusicMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserSongService {

    private final UserSongRepository userSongRepository;
    private final UsuarioRepository usuarioRepository;
    private final MusicService musicService;
    private final MusicMapper musicMapper;

    public UserSong adicionarMusica(Long userId, com.joaopaulo.musicas.dtos.request.MusicSaveRequest request) {
        validarPropriedadeDoUsuario(userId);
        String trackId = request.getTrackId();
        
        if (trackId == null || trackId.isBlank() || trackId.equalsIgnoreCase("undefined")) {
            throw new IllegalArgumentException("ID da música inválido");
        }

        if (!usuarioRepository.existsById(java.util.Objects.requireNonNull(userId))) {

            throw new UsuarioNaoEncontradoException("Usuário não encontrado");
        }

        Optional<UserSong> existente = userSongRepository.findByUserIdAndTrackId(userId, trackId);
        if (existente.isPresent()) {
            log.info("Música {} já está na lista do usuário {}", trackId, userId);
            return existente.get();
        }

        // Garante que a música existe no catálogo local antes de vincular ao usuário
        musicService.saveCustomMusic(request);

        UserSong userSong = UserSong.builder()
                .userId(userId)
                .trackId(trackId)
                .source(request.getSource())
                .dataAdicao(LocalDateTime.now())
                .trackName(request.getNome())
                .artistName(request.getArtista())
                .albumName(request.getAlbum())
                .build();

        return userSongRepository.save(java.util.Objects.requireNonNull(userSong));

    }

    public void removerMusica(Long userId, String trackId) {
        validarPropriedadeDoUsuario(userId);
        userSongRepository.deleteByUserIdAndTrackId(userId, trackId);
    }

    public Long removerMusicasPorFonte(Long userId, MusicSource source) {
        validarPropriedadeDoUsuario(userId);
        
        // 1. Deleta pelo campo source explicitamente (funciona para novas importações)
        Long count = userSongRepository.deleteByUserIdAndSource(userId, source);
        
        // 2. Fallback Heurístico: Remove registros antigos que não tinham o campo source preenchido
        // mas que são claramente do Spotify (ID alfanumérico longo)
        if (source == MusicSource.SPOTIFY) {
            List<UserSong> allSongs = userSongRepository.findAllByUserId(userId);
            List<UserSong> legacySpotifySongs = allSongs.stream()
                    .filter(us -> us.getSource() == null && isSpotifyLikeId(us.getTrackId()))
                    .toList();
            
            if (!legacySpotifySongs.isEmpty()) {
                userSongRepository.deleteAll(legacySpotifySongs);
                count += legacySpotifySongs.size();
                log.info("Removidas {} músicas legadas do Spotify (id heurístico) para o usuário {}", legacySpotifySongs.size(), userId);
            }
        }

        log.info("Sincronização removida com sucesso: total de {} músicas da fonte {} apagadas para o usuário {}", count, source, userId);
        return count;
    }


    private boolean isSpotifyLikeId(String trackId) {
        if (trackId == null) return false;
        // IDs do Spotify são alfanuméricos (letras e números) de ~22 caracteres.
        // IDs da Apple/iTunes são puramente numéricos.
        return trackId.length() > 10 && trackId.matches("^[a-zA-Z0-9]+$") && !trackId.matches("^\\d+$");
    }




    public List<String> listarIdsMusicasCurtidas(Long userId) {
        validarPropriedadeDoUsuario(userId);
        return userSongRepository.findTrackIdsByUserId(userId)
                .stream()
                .map(UserSong::getTrackId)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    public Page<UserSongResponse> listarMusicas(Long userId, Pageable pageable) {
        return listarMusicas(userId, null, pageable);
    }

    public Page<UserSongResponse> listarMusicas(Long userId, String searchTerm, Pageable pageable) {
        validarPropriedadeDoUsuario(userId);

        if (!usuarioRepository.existsById(java.util.Objects.requireNonNull(userId))) {
            throw new UsuarioNaoEncontradoException("Usuário não encontrado");
        }
        
        Page<UserSong> page;
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            page = userSongRepository.findByUserIdAndSearchTerm(userId, searchTerm.trim(), pageable);
        } else {
            page = userSongRepository.findByUserId(userId, pageable);
        }

        return page.map(us -> {
                    com.joaopaulo.musicas.dtos.response.MusicResponse musicResponse;
                    try {
                        var musicEntity = musicService.findById(us.getTrackId());
                        musicResponse = musicMapper.toResponse(musicEntity);

                        // Auto-reparo de metadados para busca/ordenação caso estejam faltando (dados legados)
                        if (us.getTrackName() == null || us.getArtistName() == null) {
                            us.setTrackName(musicEntity.getNome());
                            us.setArtistName(musicEntity.getArtista());
                            us.setAlbumName(musicEntity.getAlbum());
                            userSongRepository.save(us);
                            log.info("Metadados reparados para UserSong ID: {} (Música: {})", us.getId(), us.getTrackName());
                        }

                    } catch (com.joaopaulo.musicas.exceptions.MusicNotFoundException e) {
                        log.warn("Música {} não encontrada no catálogo para exibição na biblioteca.", us.getTrackId());
                        musicResponse = com.joaopaulo.musicas.dtos.response.MusicResponse.builder()
                                .id(us.getTrackId())
                                .nome("Música indisponível")
                                .artista("Artista desconhecido")
                                .album("Álbum desconhecido")
                                .capaUrl("")
                                .anoLancamento(null)
                                .previewUrl("")
                                .source(us.getSource())
                                .build();
                    }
                    
                    return new UserSongResponse(
                            us.getId(),
                            us.getUserId(),
                            musicResponse,
                            us.getDataAdicao()
                    );
                });
    }

    public Optional<UserSong> verificarMusicaSalva(Long userId, String trackId) {
        validarPropriedadeDoUsuario(userId);
        return userSongRepository.findByUserIdAndTrackId(userId, trackId);
    }

    private void validarPropriedadeDoUsuario(Long userId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !(auth.getPrincipal() instanceof UsuarioDetails usuarioDetails)) {
            throw new AccessDeniedException("Usuário não autenticado");
        }

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !usuarioDetails.getId().equals(userId)) {
            log.warn("Tentativa de IDOR: usuário {} tentou acessar dados do usuário {}",
                    usuarioDetails.getId(), userId);
            throw new IdorSecurityException("Você não tem permissão para acessar o perfil de outro usuário");
        }
    }
}