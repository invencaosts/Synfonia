package com.synfonia.musicas.controllers;

import com.synfonia.musicas.exceptions.UnauthorizedException;
import com.synfonia.musicas.security.UsuarioDetails;
import com.synfonia.musicas.services.YtMusicAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ytmusic")
@RequiredArgsConstructor
@Tag(name = "YouTube Music", description = "Conexão OAuth (device code, estilo TV) com a conta pessoal do YouTube Music")
public class YtMusicAuthController {

    private final RestClient restClient;
    private final YtMusicAuthService ytMusicAuthService;

    @Value("${ytmusic.service.url}")
    private String ytMusicServiceUrl;

    private Long requireLoggedUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken
                || !(authentication.getPrincipal() instanceof UsuarioDetails details)) {
            throw new UnauthorizedException("Você precisa estar logado para usar o YouTube Music.");
        }
        return details.getId();
    }

    @Operation(summary = "Diz se o usuário logado já conectou uma conta do YouTube Music")
    @PostMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        Long userId = requireLoggedUserId();
        return ResponseEntity.ok(Map.of("connected", ytMusicAuthService.isConnected(userId)));
    }

    @Operation(summary = "Desconecta a conta do YouTube Music do usuário logado")
    @PostMapping("/disconnect")
    public ResponseEntity<Void> disconnect() {
        Long userId = requireLoggedUserId();
        ytMusicAuthService.disconnect(userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Inicia o fluxo OAuth device-code do YouTube Music (retorna o código pro usuário digitar em google.com/device)")
    @PostMapping("/auth/device/start")
    public ResponseEntity<Map<String, Object>> startDeviceAuth() {
        requireLoggedUserId();
        var response = restClient.post()
                .uri(ytMusicServiceUrl + "/auth/device/start")
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Verifica se o usuário já autorizou o device code; se sim, salva a credencial no backend")
    @PostMapping("/auth/device/poll")
    public ResponseEntity<Map<String, Object>> pollDeviceAuth(@RequestBody Map<String, Object> body) {
        Long userId = requireLoggedUserId();
        var response = restClient.post()
                .uri(ytMusicServiceUrl + "/auth/device/poll")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});

        if ("authorized".equals(response.get("status"))) {
            @SuppressWarnings("unchecked")
            Map<String, Object> token = (Map<String, Object>) response.get("token");
            ytMusicAuthService.saveFromDeviceToken(userId, token);
            // Nunca devolve o token pro navegador: só a confirmação de status.
            return ResponseEntity.ok(Map.of("status", "authorized"));
        }

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Retorna nome/handle/foto da conta do YouTube Music conectada")
    @PostMapping("/me/account")
    public ResponseEntity<Map<String, Object>> getAccount() {
        Long userId = requireLoggedUserId();
        var token = ytMusicAuthService.getValidToken(userId);
        var response = restClient.post()
                .uri(ytMusicServiceUrl + "/me/account")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("token", token))
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Lista as playlists da conta do YouTube Music conectada")
    @PostMapping("/me/playlists")
    public ResponseEntity<List<Map<String, Object>>> getMyPlaylists() {
        Long userId = requireLoggedUserId();
        var token = ytMusicAuthService.getValidToken(userId);
        var response = restClient.post()
                .uri(ytMusicServiceUrl + "/me/playlists")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("token", token))
                .retrieve()
                .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Lista as faixas de uma playlist específica da conta do YouTube Music conectada")
    @PostMapping("/me/playlists/{playlistId}/tracks")
    public ResponseEntity<List<Map<String, Object>>> getPlaylistTracks(@PathVariable("playlistId") String playlistId) {
        Long userId = requireLoggedUserId();
        var token = ytMusicAuthService.getValidToken(userId);
        var response = restClient.post()
                .uri(ytMusicServiceUrl + "/me/playlists/{playlistId}/tracks", playlistId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("token", token))
                .retrieve()
                .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Lista as músicas curtidas na conta do YouTube Music conectada")
    @PostMapping("/me/liked-songs")
    public ResponseEntity<List<Map<String, Object>>> getMyLikedSongs() {
        Long userId = requireLoggedUserId();
        var token = ytMusicAuthService.getValidToken(userId);
        var response = restClient.post()
                .uri(ytMusicServiceUrl + "/me/liked-songs")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("token", token))
                .retrieve()
                .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
        return ResponseEntity.ok(response);
    }
}
