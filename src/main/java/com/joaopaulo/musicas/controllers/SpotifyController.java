package com.joaopaulo.musicas.controllers;

import com.joaopaulo.musicas.dtos.response.SpotifyTokenResponse;
import com.joaopaulo.musicas.exceptions.SpotifyApiException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.Base64;

@RestController
@RequestMapping("/api/v1/spotify")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Spotify Auth", description = "Endpoints para integração com a autenticação do Spotify")
public class SpotifyController {

    private final RestClient restClient;

    @Value("${spotify.client.id}")
    private String clientId;

    @Value("${spotify.client.secret}")
    private String clientSecret;

    @Value("${spotify.redirect.uri}")
    private String redirectUri;

    @Operation(summary = "Troca o código de autorização por um token de acesso")
    @GetMapping("/callback")
    public ResponseEntity<SpotifyTokenResponse> callback(@RequestParam("code") String code) {
        log.info("Recebido código de autorização do Spotify, iniciando troca por token.");

        String authHeader = "Basic " + Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes());

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "authorization_code");
        formData.add("code", code);
        formData.add("redirect_uri", redirectUri);

        log.info("[Spotify Auth] Trocando código. ClientID: {}. RedirectURI: {}", clientId, redirectUri);
        
        try {
            SpotifyTokenResponse response = restClient.post()
                    .uri("https://accounts.spotify.com/api/token")
                    .header("Authorization", authHeader)
                    .contentType(java.util.Objects.requireNonNull(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED))
                    .body(formData)


                    .retrieve()
                    .body(SpotifyTokenResponse.class);

            if (response != null && response.access_token() != null) {
                log.info("[Spotify Auth] Sucesso! Token recebido.");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erro ao trocar código por token no Spotify: {}", e.getMessage());
            throw new SpotifyApiException("Erro na autenticação com Spotify: " + e.getMessage(), e);
        }
    }
}
