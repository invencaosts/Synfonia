package com.synfonia.musicas.services;

import com.synfonia.musicas.dtos.response.PublicAlbumRatingResponse;
import com.synfonia.musicas.entities.AlbumRating;
import com.synfonia.musicas.entities.Usuario;
import com.synfonia.musicas.exceptions.AlbumRatingNotFoundException;
import com.synfonia.musicas.repositories.AlbumRatingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Link público de compartilhamento de uma avaliação (o QR code / link da imagem gerada no app).
 *
 * Mesma regra de visibilidade da comunidade: a avaliação só abre se não estiver oculta pela
 * moderação e se o autor estiver ativo, não suspenso, com perfil público e avaliações visíveis.
 * Fora disso responde 404, sem dizer o motivo.
 */
@Service
public class PublicRatingService {

    static final String ANDROID_PACKAGE = "com.synfonia.app";
    static final String DEEP_LINK_SCHEME = "synfonia";
    static final String RELEASES_URL = "https://github.com/invencaosts/Synfonia-Front/releases/latest";

    private static final String NAO_ENCONTRADA = "Avaliação não encontrada ou não está pública.";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String TOKEN_CONTEXT = "synfonia:public-rating:";
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("pt-BR"));

    private final AlbumRatingRepository albumRatingRepository;
    private final byte[] signingSecret;

    public PublicRatingService(
            AlbumRatingRepository albumRatingRepository,
            @Value("${app.public-rating.secret}") String signingSecret) {
        this.albumRatingRepository = albumRatingRepository;
        if (signingSecret == null || signingSecret.isBlank()) {
            throw new IllegalStateException("app.public-rating.secret não pode ser vazio");
        }
        this.signingSecret = signingSecret.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Gera um token apenas se a avaliação pertencer ao usuário autenticado. A consulta composta
     * também evita revelar se um ID válido pertence a outra pessoa.
     */
    @Transactional(readOnly = true)
    public String gerarTokenDoDono(String id, Long userId) {
        albumRatingRepository.findByIdAndUserId(id, userId)
                .filter(PublicRatingService::visivel)
                .orElseThrow(() -> new AlbumRatingNotFoundException(NAO_ENCONTRADA));
        return gerarToken(id);
    }

    @Transactional(readOnly = true)
    public PublicAlbumRatingResponse buscar(String id, String token) {
        if (!tokenValido(id, token)) {
            throw new AlbumRatingNotFoundException(NAO_ENCONTRADA);
        }
        AlbumRating rating = albumRatingRepository.findComUsuarioById(id)
                .filter(PublicRatingService::visivel)
                .orElseThrow(() -> new AlbumRatingNotFoundException(NAO_ENCONTRADA));
        Usuario autor = rating.getUsuario();

        return PublicAlbumRatingResponse.builder()
                .id(rating.getId())
                .artista(rating.getArtista())
                .albumName(rating.getAlbumName())
                .capaUrl(rating.getCapaUrl())
                .nota(rating.getNota())
                .titulo(rating.getTitulo())
                .review(rating.getReview())
                .criadoEm(rating.getCriadoEm())
                .atualizadoEm(rating.getAtualizadoEm())
                .username(autor.getUsername())
                .displayName(autor.getDisplayName())
                .fotoPerfil(autor.getFotoPerfil())
                .build();
    }

    String gerarToken(String id) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(assinar(id));
    }

    boolean tokenValido(String id, String token) {
        if (id == null || id.isBlank() || token == null || token.isBlank()) {
            return false;
        }
        try {
            byte[] informado = Base64.getUrlDecoder().decode(token);
            return MessageDigest.isEqual(assinar(id), informado);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private byte[] assinar(String id) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(signingSecret, HMAC_ALGORITHM));
            return mac.doFinal((TOKEN_CONTEXT + id).getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 indisponível", e);
        }
    }

    private static boolean visivel(AlbumRating rating) {
        Usuario autor = rating.getUsuario();
        return !Boolean.TRUE.equals(rating.getOculto())
                && autor != null
                && autor.isAtivo()
                && !autor.isBanidoAgora()
                && !Boolean.FALSE.equals(autor.getPerfilPublico())
                && !Boolean.FALSE.equals(autor.getShowAvaliacoes());
    }

    /**
     * Página HTML server-side (o front é só o app, não há site hospedado). Também serve de prévia
     * (Open Graph) quando o link é colado no WhatsApp/Twitter. Todo texto do usuário passa por
     * htmlEscape; a capa só entra se for http(s).
     */
    public String renderizarPagina(PublicAlbumRatingResponse r, String urlPagina, String token) {
        String album = esc(r.getAlbumName());
        String artista = esc(r.getArtista());
        String autor = esc(r.getUsername());
        String nota = r.getNota() == null ? "" : String.format(Locale.US, "%.1f", r.getNota());
        String capa = capaSegura(r.getCapaUrl());
        String tituloPagina = "@" + autor + " avaliou " + album + " — Synfonia";
        String descricao = r.getTitulo() != null && !r.getTitulo().isBlank()
                ? esc(r.getTitulo())
                : "Nota " + nota + " de 5 para " + album + ", de " + artista + ".";
        String deepLink = "intent://avaliacao/" + UriUtils.encodePathSegment(r.getId(), StandardCharsets.UTF_8)
                + "?token=" + UriUtils.encodeQueryParam(token, StandardCharsets.UTF_8)
                + "#Intent;scheme=" + DEEP_LINK_SCHEME + ";package=" + ANDROID_PACKAGE
                + ";S.browser_fallback_url=" + UriUtils.encode(RELEASES_URL, StandardCharsets.UTF_8) + ";end";
        var data = r.getAtualizadoEm() != null ? r.getAtualizadoEm() : r.getCriadoEm();

        StringBuilder html = new StringBuilder(4096);
        html.append("<!doctype html><html lang=\"pt-BR\"><head><meta charset=\"utf-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
                .append("<title>").append(tituloPagina).append("</title>")
                .append("<meta name=\"description\" content=\"").append(descricao).append("\">")
                .append("<meta property=\"og:type\" content=\"article\">")
                .append("<meta property=\"og:site_name\" content=\"Synfonia\">")
                .append("<meta property=\"og:title\" content=\"").append(tituloPagina).append("\">")
                .append("<meta property=\"og:description\" content=\"").append(descricao).append("\">")
                .append("<meta property=\"og:url\" content=\"").append(esc(urlPagina)).append("\">");
        if (capa != null) {
            html.append("<meta property=\"og:image\" content=\"").append(capa).append("\">")
                    .append("<meta name=\"twitter:card\" content=\"summary\">");
        }
        html.append("<style>").append(CSS).append("</style></head><body><main class=\"card\">");
        if (capa != null) {
            html.append("<div class=\"fundo\" style=\"background-image:url('").append(capa).append("')\"></div>")
                    .append("<img class=\"capa\" src=\"").append(capa).append("\" alt=\"").append(album).append("\">");
        }
        html.append("<h1>").append(album).append("</h1>")
                .append("<p class=\"artista\">").append(artista).append("</p>")
                .append("<p class=\"nota\"><span class=\"estrelas\">").append(estrelas(r.getNota())).append("</span> ")
                .append(nota).append("</p>");
        if (r.getTitulo() != null && !r.getTitulo().isBlank()) {
            html.append("<p class=\"titulo\">“").append(esc(r.getTitulo())).append("”</p>");
        }
        if (r.getReview() != null && !r.getReview().isBlank()) {
            html.append("<p class=\"review\">").append(esc(r.getReview())).append("</p>");
        }
        html.append("<p class=\"autor\">por <strong>@").append(autor).append("</strong>");
        if (data != null) {
            html.append(" · ").append(DATA.format(data));
        }
        html.append("</p>")
                .append("<a class=\"botao principal\" href=\"").append(esc(deepLink)).append("\">Abrir no app Synfonia</a>")
                .append("<a class=\"botao\" href=\"").append(RELEASES_URL).append("\">Baixar o app</a>")
                .append("<p class=\"marca\">SYNFONIA</p></main></body></html>");
        return html.toString();
    }

    public String renderizarNaoEncontrada() {
        return "<!doctype html><html lang=\"pt-BR\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                + "<title>Avaliação não encontrada — Synfonia</title><style>" + CSS + "</style></head>"
                + "<body><main class=\"card\"><h1>Avaliação não encontrada</h1>"
                + "<p class=\"artista\">Ela pode ter sido apagada ou não está mais pública.</p>"
                + "<a class=\"botao\" href=\"" + RELEASES_URL + "\">Baixar o app</a>"
                + "<p class=\"marca\">SYNFONIA</p></main></body></html>";
    }

    private static String esc(String texto) {
        return texto == null ? "" : HtmlUtils.htmlEscape(texto, StandardCharsets.UTF_8.name());
    }

    private static String capaSegura(String url) {
        if (url == null) return null;
        String limpa = url.trim();
        String minuscula = limpa.toLowerCase(Locale.ROOT);
        if (!minuscula.startsWith("https://") && !minuscula.startsWith("http://")) return null;
        // Vai dentro de url('...') no CSS inline: aspas/parênteses/barra invertida quebrariam o contexto
        if (limpa.matches(".*['\"()\\\\\\s<>].*")) return null;
        return esc(limpa);
    }

    private static String estrelas(Double nota) {
        int cheias = nota == null ? 0 : (int) Math.round(nota);
        cheias = Math.max(0, Math.min(5, cheias));
        return "★".repeat(cheias) + "<span class=\"vazia\">" + "★".repeat(5 - cheias) + "</span>";
    }

    private static final String CSS = """
            *{box-sizing:border-box}
            body{margin:0;min-height:100vh;display:flex;align-items:center;justify-content:center;padding:24px 16px;
            background:#09090b;color:#fff;font-family:system-ui,-apple-system,'Segoe UI',Roboto,sans-serif}
            .card{position:relative;overflow:hidden;width:100%;max-width:440px;padding:32px 24px;border-radius:24px;
            background:#18181b;text-align:center;display:flex;flex-direction:column;align-items:center;gap:12px}
            .card>*{position:relative}
            .fundo{position:absolute;inset:-40px;background-size:cover;background-position:center;filter:blur(32px);opacity:.35}
            .capa{width:180px;height:180px;border-radius:16px;object-fit:cover;box-shadow:0 10px 30px rgba(0,0,0,.5)}
            h1{margin:8px 0 0;font-size:22px;line-height:1.2;overflow-wrap:anywhere}
            .artista{margin:0;color:rgba(255,255,255,.7)}
            .nota{margin:4px 0;font-weight:700;font-size:18px}
            .estrelas{color:#8b5cf6;letter-spacing:2px}.vazia{color:rgba(255,255,255,.25)}
            .titulo{margin:4px 0 0;font-style:italic;font-size:17px;color:rgba(255,255,255,.9);overflow-wrap:anywhere}
            .review{margin:0;white-space:pre-line;text-align:left;width:100%;line-height:1.55;color:rgba(255,255,255,.8);
            background:rgba(0,0,0,.3);padding:14px 16px;border-radius:16px;overflow-wrap:anywhere}
            .autor{margin:4px 0 8px;font-size:14px;color:rgba(255,255,255,.6)}.autor strong{color:#fff}
            .botao{display:block;width:100%;padding:14px;border-radius:999px;text-decoration:none;font-weight:700;
            color:#fff;background:rgba(255,255,255,.1)}
            .botao.principal{background:#8b5cf6}
            .marca{margin:12px 0 0;font-size:11px;font-weight:700;letter-spacing:1px;color:rgba(255,255,255,.4)}
            """;
}
