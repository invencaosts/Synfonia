package com.synfonia.musicas.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synfonia.musicas.entities.YtMusicCredential;
import com.synfonia.musicas.exceptions.UnauthorizedException;
import com.synfonia.musicas.repositories.YtMusicCredentialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Guarda e renova a credencial OAuth do YouTube Music no backend — o access/refresh token
 * nunca é devolvido ao navegador (evita expor um token de escopo amplo a um eventual XSS).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class YtMusicAuthService {

    // Cliente HTTP dedicado (java.net.http), sem passar pelos conversores
    // customizados do RestClient compartilhado (que quebravam o corpo JSON
    // dessas chamadas de forma silenciosa).
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            // uvicorn só fala HTTP/1.1; a negociação HTTP/2 padrão do HttpClient
            // confundia o parser dele ("Invalid HTTP request received").
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    private final YtMusicCredentialRepository credentialRepository;
    private final ObjectMapper objectMapper;

    @Value("${ytmusic.service.url}")
    private String ytMusicServiceUrl;

    public boolean isConnected(Long usuarioId) {
        return credentialRepository.existsById(usuarioId);
    }

    public void saveFromDeviceToken(Long usuarioId, Map<String, Object> token) {
        credentialRepository.save(toEntity(usuarioId, token));
    }

    public void disconnect(Long usuarioId) {
        credentialRepository.deleteById(usuarioId);
    }

    /**
     * Retorna um token pronto pra uso, renovando primeiro se estiver perto de expirar.
     */
    public Map<String, Object> getValidToken(Long usuarioId) {
        YtMusicCredential credential = credentialRepository.findById(usuarioId)
                .orElseThrow(() -> new UnauthorizedException("Conta do YouTube Music não conectada."));

        boolean isExpiring = credential.getExpiresAt() - Instant.now().getEpochSecond() < 60;
        if (isExpiring) {
            credential = refresh(credential);
        }

        return toMap(credential);
    }

    private YtMusicCredential refresh(YtMusicCredential credential) {
        try {
            String json = objectMapper.writeValueAsString(Map.of("refresh_token", credential.getRefreshToken()));
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(ytMusicServiceUrl + "/auth/refresh"))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> httpResponse = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (httpResponse.statusCode() >= 400) {
                throw new RuntimeException("YtMusicService /auth/refresh respondeu " + httpResponse.statusCode() + ": " + httpResponse.body());
            }

            JsonNode responseNode = objectMapper.readTree(httpResponse.body());
            Map<String, Object> token = objectMapper.convertValue(responseNode.get("token"), new TypeReference<Map<String, Object>>() {});

            YtMusicCredential updated = toEntity(credential.getUsuarioId(), token);
            return credentialRepository.save(updated);
        } catch (Exception e) {
            log.warn("[YtMusic] Falha ao renovar token do usuário {}: {}", credential.getUsuarioId(), e.getMessage());
            credentialRepository.deleteById(credential.getUsuarioId());
            throw new UnauthorizedException("Sessão do YouTube Music expirou. Conecte novamente.");
        }
    }

    private YtMusicCredential toEntity(Long usuarioId, Map<String, Object> token) {
        return YtMusicCredential.builder()
                .usuarioId(usuarioId)
                .accessToken((String) token.get("access_token"))
                .refreshToken((String) token.get("refresh_token"))
                .expiresAt(((Number) token.get("expires_at")).longValue())
                .scope((String) token.get("scope"))
                .tokenType((String) token.get("token_type"))
                .build();
    }

    private Map<String, Object> toMap(YtMusicCredential credential) {
        Map<String, Object> token = new HashMap<>();
        token.put("access_token", credential.getAccessToken());
        token.put("refresh_token", credential.getRefreshToken());
        token.put("expires_at", credential.getExpiresAt());
        token.put("scope", credential.getScope());
        token.put("token_type", credential.getTokenType());
        return token;
    }
}
