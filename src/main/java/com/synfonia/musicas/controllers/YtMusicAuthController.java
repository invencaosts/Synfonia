package com.synfonia.musicas.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synfonia.musicas.exceptions.UnauthorizedException;
import com.synfonia.musicas.security.UsuarioDetails;
import com.synfonia.musicas.services.YtMusicAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ytmusic")
@RequiredArgsConstructor
@Tag(name = "YouTube Music", description = "Conexão OAuth (device code, estilo TV) com a conta pessoal do YouTube Music")
public class YtMusicAuthController {

    // Cliente HTTP dedicado (java.net.http), sem passar pelos conversores
    // customizados do RestClient compartilhado (que quebravam o corpo JSON
    // dessas chamadas de forma silenciosa).
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            // uvicorn só fala HTTP/1.1; a negociação HTTP/2 padrão do HttpClient
            // confundia o parser dele ("Invalid HTTP request received").
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    private final YtMusicAuthService ytMusicAuthService;
    private final ObjectMapper objectMapper;

    @Value("${ytmusic.service.url}")
    private String ytMusicServiceUrl;

    private JsonNode postJson(String path, Object body) {
        try {
            String json = objectMapper.writeValueAsString(body != null ? body : Map.of());
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(ytMusicServiceUrl + path))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new RuntimeException("YtMusicService " + path + " respondeu " + response.statusCode() + ": " + response.body());
            }
            return objectMapper.readTree(response.body());
        } catch (IOException e) {
            throw new RuntimeException("Falha ao chamar YtMusicService (" + path + "): " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Chamada ao YtMusicService interrompida", e);
        }
    }

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
        JsonNode response = postJson("/auth/device/start", null);
        return ResponseEntity.ok(objectMapper.convertValue(response, new TypeReference<Map<String, Object>>() {}));
    }

    @Operation(summary = "Verifica se o usuário já autorizou o device code; se sim, salva a credencial no backend")
    @PostMapping("/auth/device/poll")
    public ResponseEntity<Map<String, Object>> pollDeviceAuth(@RequestBody Map<String, Object> body) {
        Long userId = requireLoggedUserId();
        JsonNode response = postJson("/auth/device/poll", body);

        if ("authorized".equals(response.path("status").asText())) {
            Map<String, Object> token = objectMapper.convertValue(response.get("token"), new TypeReference<Map<String, Object>>() {});
            ytMusicAuthService.saveFromDeviceToken(userId, token);
            // Nunca devolve o token pro navegador: só a confirmação de status.
            return ResponseEntity.ok(Map.of("status", "authorized"));
        }

        return ResponseEntity.ok(objectMapper.convertValue(response, new TypeReference<Map<String, Object>>() {}));
    }

    @Operation(summary = "Retorna nome/handle/foto da conta do YouTube Music conectada")
    @PostMapping("/me/account")
    public ResponseEntity<Map<String, Object>> getAccount() {
        Long userId = requireLoggedUserId();
        var token = ytMusicAuthService.getValidToken(userId);
        JsonNode response = postJson("/me/account", Map.of("token", token));
        return ResponseEntity.ok(objectMapper.convertValue(response, new TypeReference<Map<String, Object>>() {}));
    }

}
