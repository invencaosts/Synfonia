package com.synfonia.musicas.controllers;

import com.synfonia.musicas.dtos.response.PublicAlbumRatingResponse;
import com.synfonia.musicas.exceptions.AlbumRatingNotFoundException;
import com.synfonia.musicas.services.PublicRatingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicRatingControllerTest {

    private static final String ID = "abc-123";
    private static final String TOKEN = "token-valido";

    @Mock private PublicRatingService publicRatingService;

    private PublicRatingController controller;

    @BeforeEach
    void setUp() {
        controller = new PublicRatingController(publicRatingService);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void apiPublicaExigeTokenEUsaNoStore() {
        when(publicRatingService.buscar(ID, TOKEN)).thenReturn(PublicAlbumRatingResponse.builder().id(ID).build());

        var resposta = controller.buscar(ID, TOKEN);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getHeaders().getCacheControl()).isEqualTo("no-store");
        verify(publicRatingService).buscar(ID, TOKEN);
    }

    @Test
    void tokenInvalidoRetorna404IndistinguivelENoStore() {
        when(publicRatingService.buscar(ID, "invalido"))
                .thenThrow(new AlbumRatingNotFoundException("Avaliação não encontrada ou não está pública."));

        var resposta = controller.buscar(ID, "invalido");

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resposta.getHeaders().getCacheControl()).isEqualTo("no-store");
        assertThat(resposta.getBody()).isNull();
    }

    @Test
    void paginaPreservaTokenENaoPermiteCache() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/avaliacao/" + ID);
        request.setQueryString("token=" + TOKEN);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        PublicAlbumRatingResponse rating = PublicAlbumRatingResponse.builder().id(ID).build();
        when(publicRatingService.buscar(ID, TOKEN)).thenReturn(rating);
        when(publicRatingService.renderizarPagina(rating, "http://localhost/avaliacao/" + ID + "?token=" + TOKEN, TOKEN))
                .thenReturn("<html>ok</html>");

        var resposta = controller.pagina(ID, TOKEN);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getHeaders().getCacheControl()).isEqualTo("no-store");
        assertThat(resposta.getBody()).isEqualTo("<html>ok</html>");
    }

    @Test
    void paginaCurtaRenderizaComTokenParaOApp() {
        String codigo = "Abc12345";
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/a/" + codigo);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        PublicAlbumRatingResponse rating = PublicAlbumRatingResponse.builder().id(ID).build();
        when(publicRatingService.buscarPorCodigo(codigo)).thenReturn(rating);
        when(publicRatingService.gerarToken(ID)).thenReturn(TOKEN);
        when(publicRatingService.renderizarPagina(rating, "http://localhost/a/" + codigo, TOKEN))
                .thenReturn("<html>ok</html>");

        var resposta = controller.paginaCurta(codigo);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getHeaders().getCacheControl()).isEqualTo("no-store");
        assertThat(resposta.getBody()).isEqualTo("<html>ok</html>");
    }

    @Test
    void codigoInexistenteRetorna404() {
        when(publicRatingService.buscarPorCodigo("Inexiste"))
                .thenThrow(new AlbumRatingNotFoundException("Avaliação não encontrada ou não está pública."));

        assertThat(controller.buscarPorCodigo("Inexiste").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
