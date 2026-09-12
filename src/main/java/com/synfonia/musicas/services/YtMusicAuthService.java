package com.synfonia.musicas.services;

import com.synfonia.musicas.entities.YtMusicCredential;
import com.synfonia.musicas.exceptions.UnauthorizedException;
import com.synfonia.musicas.repositories.YtMusicCredentialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

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

    private final YtMusicCredentialRepository credentialRepository;
    private final RestClient restClient;

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
            var response = restClient.post()
                    .uri(ytMusicServiceUrl + "/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("refresh_token", credential.getRefreshToken()))
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            @SuppressWarnings("unchecked")
            Map<String, Object> token = (Map<String, Object>) response.get("token");

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
