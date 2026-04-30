package com.joaopaulo.musicas.services;

import com.joaopaulo.musicas.entities.UserSong;
import com.joaopaulo.musicas.exceptions.UsuarioNaoEncontradoException;
import com.joaopaulo.musicas.repositories.UserSongRepository;
import com.joaopaulo.musicas.repositories.UsuarioRepository;
import com.joaopaulo.musicas.dtos.response.UserSongResponse;
import com.joaopaulo.musicas.enums.MusicSource;
import com.joaopaulo.musicas.mappers.MusicMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

    @SuppressWarnings("null")
    public UserSong adicionarMusica(com.joaopaulo.musicas.dtos.request.MusicSaveRequest request) {
        Long userId = getLoggedUserId();
        String trackId = request.getTrackId();
        
        if (trackId == null || trackId.isBlank() || trackId.equalsIgnoreCase("undefined")) {
            throw new IllegalArgumentException("ID da música inválido");
        }

        if (!usuarioRepository.existsById(java.util.Objects.requireNonNull(userId))) {

            throw new UsuarioNaoEncontradoException("Usuário não encontrado");
        }

        Optional<UserSong> existente = userSongRepository.findByUserIdAndTrackId(userId, trackId);
        if (existente.isPresent()) {
            log.debug("Música {} já está na lista do usuário {}", trackId, userId);
            return existente.get();
        }

        // Garante que a música existe no catálogo local antes de vincular ao usuário
        musicService.saveCustomMusic(request);

        UserSong userSong = UserSong.builder()
                .userId(userId)
                .trackId(trackId)
                .source(request.getSource())
                .trackName(request.getNome())
                .artistName(request.getArtista())
                .albumName(request.getAlbum())
                .dataAdicao(LocalDateTime.now())
                .trackName(request.getNome())
                .artistName(request.getArtista())
                .albumName(request.getAlbum())
                .build();

        return userSongRepository.save(java.util.Objects.requireNonNull(userSong));

    }

    public void removerMusica(String trackId) {
        Long userId = getLoggedUserId();
        userSongRepository.deleteByUserIdAndTrackId(userId, trackId);
    }

    public Long removerMusicasPorFonte(MusicSource source) {
        Long userId = getLoggedUserId();
        return userSongRepository.deleteByUserIdAndSource(userId, source);
    }

    public Page<UserSongResponse> listarMusicas(Long userId, Pageable pageable) {
        return listarMusicas(userId, null, pageable);
    }

    public Page<UserSongResponse> listarMusicas(Long userId, String searchTerm, Pageable pageable) {
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

            return new com.joaopaulo.musicas.dtos.response.UserSongResponse(
                    us.getId(),
                    us.getUserId(),
                    musicResponse,
                    us.getDataAdicao()
            );
        });
    }

    public Optional<UserSong> verificarMusicaSalva(String trackId) {
        Long userId = getLoggedUserId();
        return userSongRepository.findByUserIdAndTrackId(userId, trackId);
    }

    public java.util.List<String> getFavoriteTrackIds(Long userId, MusicSource excludeSource) {
        if (userId == null) {
            return java.util.Collections.emptyList();
        }
        if (!usuarioRepository.existsById(userId)) {
            throw new com.joaopaulo.musicas.exceptions.UsuarioNaoEncontradoException("Usuário não encontrado");
        }
        
        java.util.List<UserSong> songs;
        if (excludeSource != null) {
            songs = userSongRepository.findTrackIdsByUserIdAndSourceNot(userId, excludeSource);
        } else {
            songs = userSongRepository.findTrackIdsByUserId(userId);
        }
        
        return songs.stream()
                .map(UserSong::getTrackId)
                .toList();
    }

    public Long getLoggedUserId() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof com.joaopaulo.musicas.security.UsuarioDetails details)) {
            throw new com.joaopaulo.musicas.exceptions.UnauthorizedException("Usuário não está autenticado");
        }
        return details.getId();
    }
}