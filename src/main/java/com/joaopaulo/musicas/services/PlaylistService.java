package com.joaopaulo.musicas.services;

import com.joaopaulo.musicas.dtos.request.MusicSaveRequest;
import com.joaopaulo.musicas.entities.Playlist;
import com.joaopaulo.musicas.exceptions.PlaylistNotFoundException;
import com.joaopaulo.musicas.exceptions.SpotifyApiException;
import com.joaopaulo.musicas.exceptions.UnauthorizedException;
import com.joaopaulo.musicas.repositories.PlaylistRepository;
import com.joaopaulo.musicas.security.UsuarioDetails;
import com.joaopaulo.musicas.dtos.request.SpotifyImportDataRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Optional;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlaylistService {
    private final PlaylistRepository playlistRepository;
    private final MusicService musicService;
    private final RestClient restClient;

    private static final String PLAYLIST_NOT_FOUND = "Playlist não encontrada";

    @Value("${spotify.client.id}")
    private String clientId;

    @Value("${spotify.client.secret}")
    private String clientSecret;

    private String cachedAppToken;
    private long tokenExpiration;

    @SuppressWarnings("null")
    public Playlist create(Playlist playlist) {
        // Hardening: Garante que o usuário logado seja sempre o dono da nova playlist, 
        // independente do que vier no objeto original (Segurança por Definição).
        playlist.setUserId(getLoggedUserId());
        if (playlist.getTrackIds() == null) {
            playlist.setTrackIds(new java.util.ArrayList<>());
        }
        return playlistRepository.save(playlist);
    }

    @SuppressWarnings("null")
    public Playlist update(String id, Playlist playlistDetails) {
        checkPlaylistOwnership(id);
        Playlist playlist = playlistRepository.findById(id)
                .orElseThrow(() -> new PlaylistNotFoundException(PLAYLIST_NOT_FOUND));

        if (playlistDetails.getNome() != null) playlist.setNome(playlistDetails.getNome());
        if (playlistDetails.getVibe() != null) playlist.setVibe(playlistDetails.getVibe());
        playlist.setPublico(playlistDetails.isPublico());
        if (playlistDetails.getCapaUrl() != null) playlist.setCapaUrl(playlistDetails.getCapaUrl());

        return playlistRepository.save(Objects.requireNonNull(playlist));
    }

    public List<Playlist> findAllByUserId(Long userId) {
        return playlistRepository.findByUserId(userId);
    }

    public List<Playlist> findPublicByUserId(Long userId) {
        return playlistRepository.findByUserIdAndPublicoTrue(userId);
    }

    @SuppressWarnings("null")
    public Optional<Playlist> findById(String id) {
        return playlistRepository.findById(Objects.requireNonNull(id));
    }

    @SuppressWarnings("null")
    public Playlist save(Playlist playlist) {
        return playlistRepository.save(Objects.requireNonNull(playlist));
    }

    @SuppressWarnings("null")
    public void delete(String id) {
        checkPlaylistOwnership(id);
        playlistRepository.deleteById(id);
    }

    @SuppressWarnings("null")
    public Playlist addTrack(String playlistId, MusicSaveRequest request) {
        checkPlaylistOwnership(playlistId);
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new PlaylistNotFoundException(PLAYLIST_NOT_FOUND));
        
        String trackId = Objects.requireNonNull(request.getTrackId());
        
        musicService.saveCustomMusic(request);

        if (!playlist.getTrackIds().contains(trackId)) {
            playlist.getTrackIds().add(trackId);
        }

        return playlistRepository.save(Objects.requireNonNull(playlist));
    }

    @SuppressWarnings("null")
    public Playlist addTracks(String playlistId, List<MusicSaveRequest> requests) {
        checkPlaylistOwnership(playlistId);
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new PlaylistNotFoundException(PLAYLIST_NOT_FOUND));
        
        if (playlist.getTrackIds() == null) {
            playlist.setTrackIds(new java.util.ArrayList<>());
        }
        
        for (var request : requests) {
            String trackId = Objects.requireNonNull(request.getTrackId());
            musicService.saveCustomMusic(request);
            if (!playlist.getTrackIds().contains(trackId)) {
                playlist.getTrackIds().add(trackId);
            }
        }

        log.info("[Import Diagnostic] Finalizando addTracks. Playlist {} agora tem {} músicas.", 
                playlistId, playlist.getTrackIds().size());
        return playlistRepository.save(Objects.requireNonNull(playlist));
    }

    @SuppressWarnings("null")
    public Playlist removeTrack(String playlistId, String trackId) {
        checkPlaylistOwnership(playlistId);
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new PlaylistNotFoundException(PLAYLIST_NOT_FOUND));

        playlist.getTrackIds().remove(Objects.requireNonNull(trackId));

        return playlistRepository.save(Objects.requireNonNull(playlist));
    }

    @SuppressWarnings("null")
    @org.springframework.transaction.annotation.Transactional
    public Playlist importPlaylistData(com.joaopaulo.musicas.dtos.request.SpotifyImportDataRequest request) {
        Long userId = getLoggedUserId();
        
        List<com.joaopaulo.musicas.dtos.request.SpotifyImportDataRequest.SpotifyTrackData> tracks = request.getTracks();
        log.debug("[Import Diagnostic] Recebidas {} músicas do Frontend para a playlist {}", 
                tracks != null ? tracks.size() : 0, 
                request.getName());
        
        if (tracks != null && !tracks.isEmpty()) {
            var first = tracks.get(0);
            log.debug("[Import Diagnostic] Exemplo da primeira música: ID={}, Nome={}, Artista={}, URI={}", 
                    first.getId(), first.getName(), first.getArtist(), first.getUri());
        }
        
        if (request.getTracks() != null && !request.getTracks().isEmpty()) {
            tracks = request.getTracks();
            log.info("[Import] Usando {} músicas enviadas diretamente pelo frontend (Bypass).", tracks.size());
        } else {
            try {
                tracks = fetchSpotifyTracks(request.getSpotifyPlaylistId(), request.getAccessToken());
            } catch (Exception e) {
                log.warn("[Import] Falha ao buscar músicas com token do usuário. Tentando com token do App.");
                try {
                    String appToken = getAppAccessToken();
                    tracks = fetchSpotifyTracks(request.getSpotifyPlaylistId(), appToken);
                } catch (Exception e2) {
                    log.error("[Import] Falha total na recuperação: {}", e2.getMessage());
                    throw new SpotifyApiException("Spotify negou acesso (403/404). Verifique se o ID está correto ou se a playlist é pública.", e2);
                }
            }
        }

        Playlist playlist = Playlist.builder()
                .nome(request.getName())
                .vibe("Sincronizada do Spotify")
                .publico(false)
                .userId(userId)
                .capaUrl(request.getCapaUrl())
                .syncSpotify(true)
                .spotifyPlaylistId(request.getSpotifyPlaylistId())
                .trackIds(new java.util.ArrayList<>())
                .build();

        Playlist savedPlaylist = playlistRepository.save(Objects.requireNonNull(playlist));

        if (tracks != null && !tracks.isEmpty()) {
            List<MusicSaveRequest> musicRequests = tracks.stream()
                    .map(t -> MusicSaveRequest.builder()
                            .trackId(t.getId())
                            .nome(t.getName())
                            .artista(t.getArtist())
                            .album(t.getAlbum())
                            .capaUrl(t.getCapaUrl())
                            .uri(t.getUri())
                            .source(com.joaopaulo.musicas.enums.MusicSource.SPOTIFY)
                            .build())
                    .toList();

            return addTracks(savedPlaylist.getId(), musicRequests);
        }

        return savedPlaylist;
    }

    private List<com.joaopaulo.musicas.dtos.request.SpotifyImportDataRequest.SpotifyTrackData> fetchSpotifyTracks(String spotifyPlaylistId, String token) {
        String url = "https://api.spotify.com/v1/playlists/" + spotifyPlaylistId + "/items?limit=50";
        
        try {
            var response = restClient.get()
                    .uri(url)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(JsonNode.class);
            
            java.util.List<com.joaopaulo.musicas.dtos.request.SpotifyImportDataRequest.SpotifyTrackData> tracks = new java.util.ArrayList<>();
            if (response != null && response.has("items")) {
                for (var item : response.get("items")) {
                    var trackNode = item.get("track");
                    if (trackNode == null || trackNode.isNull()) continue;
                    
                    var albumNode = trackNode.get("album");
                    tracks.add(com.joaopaulo.musicas.dtos.request.SpotifyImportDataRequest.SpotifyTrackData.builder()
                            .id(trackNode.get("id").asText())
                            .name(trackNode.get("name").asText())
                            .artist(trackNode.get("artists").get(0).get("name").asText())
                            .album(albumNode.get("name").asText())
                            .capaUrl(albumNode.get("images").has(0) ? 
                                     albumNode.get("images").get(0).get("url").asText() : null)
                            .uri(trackNode.get("uri").asText())
                            .build());
                }
            }
            return tracks;
        } catch (org.springframework.web.client.RestClientResponseException e) {
            String spotifyError = e.getResponseBodyAsString();
            log.error("Erro do Spotify (Status {}): {}", e.getStatusCode(), spotifyError);
            throw new SpotifyApiException("Spotify Status " + e.getStatusCode() + ": " + spotifyError);
        } catch (Exception e) {
            log.error("Erro genérico no fetch: {}", e.getMessage());
            throw e;
        }
    }

    @SuppressWarnings("null")
    private String getAppAccessToken() {
        if (cachedAppToken != null && System.currentTimeMillis() < tokenExpiration) {
            return cachedAppToken;
        }

        log.info("Buscando novo App Access Token (Client Credentials)");
        String auth = java.util.Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes());
        
        try {
            var response = restClient.post()
                    .uri("https://accounts.spotify.com/api/token")
                    .header("Authorization", "Basic " + auth)
                    .contentType(Objects.requireNonNull(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED))
                    .body("grant_type=client_credentials")
                    .retrieve()
                    .body(JsonNode.class);

            if (response != null && response.has("access_token")) {
                cachedAppToken = response.get("access_token").asText();
                tokenExpiration = System.currentTimeMillis() + (response.get("expires_in").asLong() * 1000) - 60000;
                return cachedAppToken;
            }
        } catch (Exception e) {
            log.error("Falha ao obter token do Spotify via Client Credentials: {}", e.getMessage());
        }
        throw new SpotifyApiException("Não foi possível obter token do Spotify");
    }


    public Long getLoggedUserId() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof com.joaopaulo.musicas.security.UsuarioDetails details)) {
            throw new UnauthorizedException("Usuário não está autenticado");
        }
        return null;
    }

    @SuppressWarnings("null")
    private void checkPlaylistOwnership(String playlistId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new PlaylistNotFoundException(PLAYLIST_NOT_FOUND));
        
        Long loggedUserId = getLoggedUserId();
        if (!playlist.getUserId().equals(loggedUserId)) {
            log.warn("Tentativa de acesso não autorizado: Usuário {} tentou modificar a playlist {} do usuário {}", 
                    loggedUserId, playlistId, playlist.getUserId());
            throw new UnauthorizedException("Você não tem permissão para modificar esta playlist");
        }
    }
}
