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
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @Operation(summary = "Callback do Spotify para troca de código por token")
    @GetMapping("/callback")
    public ResponseEntity<SpotifyTokenResponse> callback(
            @RequestParam("code") String code,
            @RequestParam(value = "state", required = false) String state) {
        
        log.info("Recebido código de autorização do Spotify, validando sessão e state.");

        // 1. Validação do Parâmetro State
        if (state == null || state.isEmpty()) {
            log.error("[Spotify Auth] Parâmetro 'state' ausente no redirecionamento.");
            throw new SpotifyApiException("A validação de segurança falhou: parâmetro 'state' ausente.");
        }

        if (!"synfonia-auth".equals(state)) {
            log.error("[Spotify Auth] State inválido recebido: {}", state);
            throw new SpotifyApiException("A validação de segurança falhou: state inválido.");
        }

        // 2. Validação da Sessão (O filtro JWT já deve ter populado o SecurityContext via Cookie)
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            log.error("[Spotify Auth] Tentativa de callback sem sessão ativa. Usuário deslogado ou cookie expirou.");
            throw new SpotifyApiException("Você precisa estar logado para conectar sua conta Spotify.");
        }

        log.info("[Spotify Auth] Sessão ({}) e State validados. Iniciando troca por token.", authentication.getName());

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
                    .contentType(java.util.Objects.requireNonNull(MediaType.APPLICATION_FORM_URLENCODED))
                    .body(formData)


                    .retrieve()
                    .body(SpotifyTokenResponse.class);

            if (response != null && response.accessToken() != null) {
                log.info("[Spotify Auth] Sucesso! Token recebido.");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erro ao trocar código por token no Spotify: {}", e.getMessage());
            throw new SpotifyApiException("Erro na autenticação com Spotify: " + e.getMessage(), e);
        }
    }
}
