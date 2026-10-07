package com.synfonia.musicas.services;

import com.synfonia.musicas.dtos.response.PublicAlbumRatingResponse;
import com.synfonia.musicas.entities.AlbumRating;
import com.synfonia.musicas.entities.Usuario;
import com.synfonia.musicas.exceptions.AlbumRatingNotFoundException;
import com.synfonia.musicas.repositories.AlbumRatingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicRatingServiceTest {

    private static final String ID = "abc-123";

    @Mock private AlbumRatingRepository albumRatingRepository;

    private PublicRatingService publicRatingService;

    private Usuario autor;
    private AlbumRating rating;

    @BeforeEach
    void setUp() {
        publicRatingService = new PublicRatingService(albumRatingRepository, "segredo-de-teste-com-tamanho-suficiente");
        autor = new Usuario();
        autor.setId(1L);
        autor.setUsername("fulano");
        autor.setAtivo(true);
        autor.setPerfilPublico(true);
        autor.setShowAvaliacoes(true);

        rating = AlbumRating.builder()
                .id(ID)
                .userId(1L)
                .usuario(autor)
                .albumName("Album")
                .artista("Artista")
                .capaUrl("https://img.example/capa.jpg")
                .nota(4.5)
                .titulo("Muito bom")
                .review("Resenha")
                .criadoEm(LocalDateTime.of(2026, 10, 1, 12, 0))
                .build();
        lenient().when(albumRatingRepository.findComUsuarioById(ID)).thenReturn(Optional.of(rating));
    }

    @Test
    void buscarDevolveAvaliacaoComUsername() {
        PublicAlbumRatingResponse r = publicRatingService.buscar(ID, publicRatingService.gerarToken(ID));

        assertThat(r.getUsername()).isEqualTo("fulano");
        assertThat(r.getReview()).isEqualTo("Resenha");
    }

    @Test
    void avaliacaoOcultaNaoAbre() {
        rating.setOculto(true);

        assertThatThrownBy(() -> publicRatingService.buscar(ID, publicRatingService.gerarToken(ID)))
                .isInstanceOf(AlbumRatingNotFoundException.class);
    }

    @Test
    void perfilPrivadoNaoAbre() {
        autor.setPerfilPublico(false);

        assertThatThrownBy(() -> publicRatingService.buscar(ID, publicRatingService.gerarToken(ID)))
                .isInstanceOf(AlbumRatingNotFoundException.class);
    }

    @Test
    void avaliacoesEscondidasNoPerfilNaoAbrem() {
        autor.setShowAvaliacoes(false);

        assertThatThrownBy(() -> publicRatingService.buscar(ID, publicRatingService.gerarToken(ID)))
                .isInstanceOf(AlbumRatingNotFoundException.class);
    }

    @Test
    void contaSuspensaNaoAbre() {
        autor.setBanido(true);

        assertThatThrownBy(() -> publicRatingService.buscar(ID, publicRatingService.gerarToken(ID)))
                .isInstanceOf(AlbumRatingNotFoundException.class);
    }

    @Test
    void donoPodeGerarToken() {
        when(albumRatingRepository.findByIdAndUserId(ID, 1L)).thenReturn(Optional.of(rating));

        String token = publicRatingService.gerarTokenDoDono(ID, 1L);

        assertThat(token).isNotBlank();
        assertThat(publicRatingService.tokenValido(ID, token)).isTrue();
    }

    @Test
    void outroUsuarioNaoPodeGerarToken() {
        when(albumRatingRepository.findByIdAndUserId(ID, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> publicRatingService.gerarTokenDoDono(ID, 2L))
                .isInstanceOf(AlbumRatingNotFoundException.class);
    }

    @Test
    void donoNaoGeraTokenQuandoAvaliacaoNaoEstaVisivel() {
        rating.setOculto(true);
        when(albumRatingRepository.findByIdAndUserId(ID, 1L)).thenReturn(Optional.of(rating));

        assertThatThrownBy(() -> publicRatingService.gerarTokenDoDono(ID, 1L))
                .isInstanceOf(AlbumRatingNotFoundException.class);
    }

    @Test
    void tokenAusenteInvalidoOuDeOutroIdNaoAbre() {
        String token = publicRatingService.gerarToken(ID);

        assertThatThrownBy(() -> publicRatingService.buscar(ID, null))
                .isInstanceOf(AlbumRatingNotFoundException.class);
        assertThatThrownBy(() -> publicRatingService.buscar(ID, "nao-e-base64***"))
                .isInstanceOf(AlbumRatingNotFoundException.class);
        assertThatThrownBy(() -> publicRatingService.buscar("outro-id", token))
                .isInstanceOf(AlbumRatingNotFoundException.class);
        verify(albumRatingRepository, never()).findComUsuarioById("outro-id");
    }

    @Test
    void paginaEscapaTextoDoUsuario() {
        rating.setTitulo("<script>alert(1)</script>");
        rating.setReview("\"><img src=x onerror=alert(1)>");

        String token = publicRatingService.gerarToken(ID);
        String html = publicRatingService.renderizarPagina(
                publicRatingService.buscar(ID, token),
                "https://x/avaliacao/" + ID + "?token=" + token,
                token);

        assertThat(html).doesNotContain("<script>alert").doesNotContain("<img src=x");
        assertThat(html).contains("&lt;script&gt;").contains("@fulano");
        assertThat(html).contains("?token=" + token + "#Intent");
        assertThat(html).contains("og:url\" content=\"https://x/avaliacao/" + ID + "?token=" + token);
    }

    @Test
    void capaComUrlInseguraEhDescartada() {
        rating.setCapaUrl("javascript:alert(1)");
        String token = publicRatingService.gerarToken(ID);
        String semEsquema = publicRatingService.renderizarPagina(publicRatingService.buscar(ID, token), "u", token);
        assertThat(semEsquema).doesNotContain("javascript:").doesNotContain("og:image");

        rating.setCapaUrl("https://x/a.jpg') ; background:url('https://evil");
        String quebraCss = publicRatingService.renderizarPagina(publicRatingService.buscar(ID, token), "u", token);
        assertThat(quebraCss).doesNotContain("evil");
    }
}
