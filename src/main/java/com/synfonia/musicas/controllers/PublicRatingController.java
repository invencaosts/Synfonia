package com.synfonia.musicas.controllers;

import com.synfonia.musicas.dtos.response.PublicAlbumRatingResponse;
import com.synfonia.musicas.exceptions.AlbumRatingNotFoundException;
import com.synfonia.musicas.services.PublicRatingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Rotas sem login usadas pelo link/QR code da imagem de compartilhamento de avaliação.
 * Liberadas no SecurityConfig.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Avaliações públicas", description = "Link de compartilhamento de avaliações (sem login)")
public class PublicRatingController {

    // CSP da página HTML: sem script nenhum; imagens só da capa (https) e estilo inline.
    private static final String CSP = "default-src 'none'; img-src https: http:; style-src 'unsafe-inline'; base-uri 'none'; form-action 'none'; frame-ancestors 'none'";

    private final PublicRatingService publicRatingService;

    // SHA-256 do certificado de assinatura do APK (keytool -list -v -keystore synfonia-release.jks),
    // separados por vírgula. Vazio = sem App Links verificados (o link abre no navegador e o botão
    // "Abrir no app" usa o esquema synfonia://).
    @Value("${app.android.cert-sha256:}")
    private String androidCertSha256;

    @Operation(summary = "Avaliação pública (para o app abrir o link compartilhado)")
    @GetMapping("/api/v1/publico/avaliacoes/{id}")
    public ResponseEntity<PublicAlbumRatingResponse> buscar(
            @PathVariable String id,
            @RequestParam(required = false) String token) {
        try {
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .body(publicRatingService.buscar(id, token));
        } catch (AlbumRatingNotFoundException e) {
            return ResponseEntity.notFound()
                    .cacheControl(CacheControl.noStore())
                    .build();
        }
    }

    @Operation(summary = "Página HTML da avaliação (destino do link/QR code da imagem compartilhada)")
    @GetMapping(value = "/avaliacao/{id}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> pagina(
            @PathVariable String id,
            @RequestParam(required = false) String token) {
        try {
            PublicAlbumRatingResponse rating = publicRatingService.buscar(id, token);
            String url = ServletUriComponentsBuilder.fromCurrentRequest().build().toUriString();
            return html(HttpStatus.OK, publicRatingService.renderizarPagina(rating, url, token));
        } catch (AlbumRatingNotFoundException e) {
            return html(HttpStatus.NOT_FOUND, publicRatingService.renderizarNaoEncontrada());
        }
    }

    @Operation(summary = "Avaliação pública pelo código do link curto (para o app abrir /a/{codigo})")
    @GetMapping("/api/v1/publico/avaliacoes/codigo/{codigo}")
    public ResponseEntity<PublicAlbumRatingResponse> buscarPorCodigo(@PathVariable String codigo) {
        try {
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .body(publicRatingService.buscarPorCodigo(codigo));
        } catch (AlbumRatingNotFoundException e) {
            return ResponseEntity.notFound()
                    .cacheControl(CacheControl.noStore())
                    .build();
        }
    }

    @Operation(summary = "Página HTML do link curto da avaliação (destino do QR code da imagem compartilhada)")
    @GetMapping(value = "/a/{codigo}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> paginaCurta(@PathVariable String codigo) {
        try {
            PublicAlbumRatingResponse rating = publicRatingService.buscarPorCodigo(codigo);
            String url = ServletUriComponentsBuilder.fromCurrentRequest().build().toUriString();
            // "Abrir no app" segue no formato longo: APKs antigos só conhecem synfonia://avaliacao/{id}?token=
            String token = publicRatingService.gerarToken(rating.getId());
            return html(HttpStatus.OK, publicRatingService.renderizarPagina(rating, url, token));
        } catch (AlbumRatingNotFoundException e) {
            return html(HttpStatus.NOT_FOUND, publicRatingService.renderizarNaoEncontrada());
        }
    }

    private static ResponseEntity<String> html(HttpStatus status, String corpo) {
        return ResponseEntity.status(status)
                .cacheControl(CacheControl.noStore())
                .header("Content-Security-Policy", CSP)
                .contentType(MediaType.parseMediaType("text/html;charset=UTF-8"))
                .body(corpo);
    }

    @GetMapping(value = "/.well-known/assetlinks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, Object>>> assetLinks() {
        List<String> digitais = Arrays.stream(androidCertSha256.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        if (digitais.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(List.of(Map.of(
                "relation", List.of("delegate_permission/common.handle_all_urls"),
                "target", Map.of(
                        "namespace", "android_app",
                        "package_name", "com.synfonia.app",
                        "sha256_cert_fingerprints", digitais))));
    }
}
