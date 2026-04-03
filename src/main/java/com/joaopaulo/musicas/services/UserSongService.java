package com.joaopaulo.musicas.services;

import com.joaopaulo.musicas.dtos.response.MusicResponse;
import com.joaopaulo.musicas.entities.UserSong;
import com.joaopaulo.musicas.exceptions.IdorSecurityException;
import com.joaopaulo.musicas.exceptions.UsuarioNaoEncontradoException;
import com.joaopaulo.musicas.repositories.UserSongRepository;
import com.joaopaulo.musicas.repositories.UsuarioRepository;
import com.joaopaulo.musicas.security.UsuarioDetails;
import com.joaopaulo.musicas.dtos.response.UserSongResponse;
import com.joaopaulo.musicas.enums.MusicSource;
import com.joaopaulo.musicas.mappers.MusicMapper;

import com.joaopaulo.musicas.services.MusicService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    public UserSong adicionarMusica(com.joaopaulo.musicas.dtos.request.MusicSaveRequest request) {
        Long userId = getLoggedUserId();
        String trackId = request.getTrackId();
        
        if (trackId == null || trackId.isBlank() || trackId.equalsIgnoreCase("undefined")) {
            throw new IllegalArgumentException("ID da música inválido");
        }

        if (!usuarioRepository.existsById(userId)) {
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
                .build();

        return userSongRepository.save(userSong);
    }

    public void removerMusica(String trackId) {
        Long userId = getLoggedUserId();
        userSongRepository.deleteByUserIdAndTrackId(userId, trackId);
    }

    public Long removerMusicasPorFonte(MusicSource source) {
        Long userId = getLoggedUserId();
        
        // 1. Deleta pelo campo source explicitamente (funciona para novas importações)
        Long count = userSongRepository.deleteByUserIdAndSource(userId, source);
        
        // 2. Fallback Heurístico: Remove registros antigos que não tinham o campo source preenchido
        // mas que são claramente do Spotify (ID alfanumérico longo)
        if (source == MusicSource.SPOTIFY) {
            List<UserSong> allSongs = userSongRepository.findByUserId(userId);
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

    public List<UserSongResponse> listarMusicas() {
        Long userId = getLoggedUserId();

        if (!usuarioRepository.existsById(userId)) {
            throw new UsuarioNaoEncontradoException("Usuário não encontrado");
        }
        
        return userSongRepository.findByUserId(userId).stream()
                .map(us -> {
                    com.joaopaulo.musicas.dtos.response.MusicResponse musicResponse;
                    try {
                        var musicEntity = musicService.findById(us.getTrackId());
                        musicResponse = musicMapper.toResponse(musicEntity);
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
                                .source(null)
                                .build();
                    }
                    
                    return new UserSongResponse(
                            us.getId(),
                            us.getUserId(),
                            musicResponse,
                            us.getDataAdicao()
                    );
                })
                .toList();
    }

    public Optional<UserSong> verificarMusicaSalva(String trackId) {
        Long userId = getLoggedUserId();
        return userSongRepository.findByUserIdAndTrackId(userId, trackId);
    }

    public Long getLoggedUserId() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof com.joaopaulo.musicas.security.UsuarioDetails details)) {
            throw new com.joaopaulo.musicas.exceptions.UnauthorizedException("Usuário não está autenticado");
        }
        return details.getId();
    }
}