package com.synfonia.musicas.services;

import com.synfonia.musicas.dtos.response.PublicProfileResponse;
import com.synfonia.musicas.entities.Playlist;
import com.synfonia.musicas.entities.Usuario;
import com.synfonia.musicas.exceptions.PlaylistNotFoundException;
import com.synfonia.musicas.exceptions.UsuarioNaoEncontradoException;
import com.synfonia.musicas.mappers.MusicMapper;
import com.synfonia.musicas.mappers.UsuarioMapper;
import com.synfonia.musicas.repositories.AlbumRatingRepository;
import com.synfonia.musicas.repositories.PlaylistRepository;
import com.synfonia.musicas.repositories.UserSongRepository;
import com.synfonia.musicas.repositories.UsuarioRepository;
import com.synfonia.musicas.security.AccessPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CommunityServiceTest {

    private static final Long DONO_ID = 1L;
    private static final Long VISITANTE_ID = 2L;

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AlbumRatingRepository albumRatingRepository;
    @Mock private PlaylistRepository playlistRepository;
    @Mock private UserSongRepository userSongRepository;
    @Mock private AlbumRatingService albumRatingService;
    @Mock private UserSongService userSongService;
    @Mock private MusicService musicService;
    @Mock private MusicMapper musicMapper;
    @Mock private UsuarioMapper usuarioMapper;
    @Mock private AccessPolicy accessPolicy;

    @InjectMocks private CommunityService communityService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder()
                .id(DONO_ID)
                .username("joao")
                .email("joao@example.com")
                .senha("hash")
                .personalName("João Silva")
                .build();
        when(usuarioRepository.findByUsernameIgnoreCase("joao")).thenReturn(Optional.of(usuario));
        when(usuarioMapper.buildSocialLinks(any())).thenReturn(Map.of());
        when(albumRatingRepository.resumoNotasVisiveisByUserId(anyLong()))
                .thenReturn(java.util.Collections.singletonList(new Object[]{2L, 3.75}));
        when(userSongRepository.countByUserId(anyLong())).thenReturn(10L);
        when(playlistRepository.countPublicasVisiveisByUserId(anyLong())).thenReturn(2L);
        when(albumRatingRepository.resumoNotasByUserId(anyLong()))
                .thenReturn(java.util.Collections.singletonList(new Object[]{3L, 4.0}));
    }

    @Test
    void perfilPublicoVisivelParaVisitanteComContadores() {
        PublicProfileResponse perfil = communityService.buscarPerfil("joao", VISITANTE_ID);

        assertThat(perfil.isProprioPerfil()).isFalse();
        assertThat(perfil.isAvaliacoesVisiveis()).isTrue();
        assertThat(perfil.getTotalAvaliacoes()).isEqualTo(2L);
        assertThat(perfil.getMediaAvaliacoes()).isEqualTo(3.8);
        assertThat(perfil.getTotalCurtidas()).isEqualTo(10L);
        assertThat(perfil.getTotalPlaylistsPublicas()).isEqualTo(2L);
        assertThat(perfil.getPersonalName()).isEqualTo("João Silva");
    }

    @Test
    void perfilPrivadoRetornaNaoEncontradoParaVisitante() {
        usuario.setPerfilPublico(false);

        assertThatThrownBy(() -> communityService.buscarPerfil("joao", VISITANTE_ID))
                .isInstanceOf(UsuarioNaoEncontradoException.class);
    }

    @Test
    void contaInativaRetornaNaoEncontradoParaVisitante() {
        usuario.setAtivo(false);

        assertThatThrownBy(() -> communityService.buscarPerfil("joao", VISITANTE_ID))
                .isInstanceOf(UsuarioNaoEncontradoException.class);
    }

    @Test
    void donoSempreVeOProprioPerfilCompleto() {
        usuario.setPerfilPublico(false);
        usuario.setShowAvaliacoes(false);
        usuario.setShowCurtidas(false);
        usuario.setShowPersonalName(false);

        PublicProfileResponse perfil = communityService.buscarPerfil("joao", DONO_ID);

        assertThat(perfil.isProprioPerfil()).isTrue();
        assertThat(perfil.isAvaliacoesVisiveis()).isTrue();
        assertThat(perfil.isCurtidasVisiveis()).isTrue();
        assertThat(perfil.getPersonalName()).isEqualTo("João Silva");
    }

    @Test
    void abasOcultasNaoExpoemContadoresNemDados() {
        usuario.setShowAvaliacoes(false);
        usuario.setShowCurtidas(false);
        usuario.setShowPersonalName(false);

        PublicProfileResponse perfil = communityService.buscarPerfil("joao", VISITANTE_ID);

        assertThat(perfil.isAvaliacoesVisiveis()).isFalse();
        assertThat(perfil.isCurtidasVisiveis()).isFalse();
        assertThat(perfil.getTotalAvaliacoes()).isNull();
        assertThat(perfil.getMediaAvaliacoes()).isNull();
        assertThat(perfil.getTotalCurtidas()).isNull();
        assertThat(perfil.getPersonalName()).isNull();

        assertThat(communityService.listarAvaliacoes("joao", VISITANTE_ID, "recentes", 0, 20)).isEmpty();
        assertThat(communityService.listarCurtidas("joao", VISITANTE_ID, 0, 20)).isEmpty();
        verify(albumRatingRepository, never()).findByUserId(anyLong(), any(Pageable.class));
        verify(albumRatingRepository, never()).findVisiveisByUserId(anyLong(), any(Pageable.class));
        verify(userSongService, never()).listarMusicas(anyLong(), any(Pageable.class));
    }

    @Test
    void playlistPrivadaDeOutroUsuarioNaoEncontrada() {
        when(playlistRepository.findByIdAndUserIdAndPublicoTrue("p1", DONO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> communityService.buscarPlaylist("joao", "p1", VISITANTE_ID))
                .isInstanceOf(PlaylistNotFoundException.class);
    }

    @Test
    void playlistPublicaNaoExpoeDadosDoSpotify() {
        Playlist playlist = Playlist.builder()
                .id("p1").userId(DONO_ID).nome("Rock").publico(true)
                .syncSpotify(true).spotifyPlaylistId("segredo")
                .trackIds(List.of("a", "b"))
                .build();
        when(playlistRepository.findPublicasVisiveisByUserId(DONO_ID)).thenReturn(List.of(playlist));

        var playlists = communityService.listarPlaylists("joao", VISITANTE_ID);

        assertThat(playlists).hasSize(1);
        assertThat(playlists.get(0).getTotalMusicas()).isEqualTo(2);
        assertThat(playlists.get(0).getTracks()).isNull();
    }

    @Test
    void buscaComTermoCurtoNaoConsultaBanco() {
        Page<?> resultado = communityService.buscarUsuarios("a", 0, 20);

        assertThat(resultado).isEmpty();
        verify(usuarioRepository, never()).buscarPerfisPublicos(any(), any());
    }

    @Test
    void buscaEscapaCuringasDoLikeERemoveArroba() {
        when(usuarioRepository.buscarPerfisPublicos(any(), any())).thenReturn(new PageImpl<>(List.of(usuario)));

        var resultado = communityService.buscarUsuarios("@jo%a_", 0, 500);

        verify(usuarioRepository).buscarPerfisPublicos(eq("jo\\%a\\_"), any(Pageable.class));
        assertThat(resultado.getContent().get(0).getUsername()).isEqualTo("joao");
    }

    @Test
    void buscaVaziaListaPerfisRecentes() {
        when(usuarioRepository.listarPerfisPublicosRecentes(any())).thenReturn(new PageImpl<>(List.of(usuario)));

        assertThat(communityService.buscarUsuarios("  ", 0, 20)).hasSize(1);
    }

    @Test
    void usernameInexistenteRetornaNaoEncontrado() {
        when(usuarioRepository.findByUsernameIgnoreCase("ninguem")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> communityService.buscarPerfil("ninguem", VISITANTE_ID))
                .isInstanceOf(UsuarioNaoEncontradoException.class);
    }

    @Test
    void usuarioSuspensoSomeParaVisitanteComum() {
        usuario.setBanido(true);

        assertThatThrownBy(() -> communityService.buscarPerfil("joao", VISITANTE_ID))
                .isInstanceOf(UsuarioNaoEncontradoException.class);
    }

    @Test
    void suspensaoVencidaVoltaASerVisivel() {
        usuario.setBanido(true);
        usuario.setBanidoAte(java.time.LocalDateTime.now().minusDays(1));

        assertThat(communityService.buscarPerfil("joao", VISITANTE_ID).getUsername()).isEqualTo("joao");
    }

    @Test
    void moderadorVeSuspensoComDadosDaSuspensao() {
        usuario.setBanido(true);
        usuario.setBanidoMotivo("spam");
        when(accessPolicy.tem(com.synfonia.musicas.enums.Permissao.USUARIOS_BANIR)).thenReturn(true);

        PublicProfileResponse perfil = communityService.buscarPerfil("joao", VISITANTE_ID);

        assertThat(perfil.getBanido()).isTrue();
        assertThat(perfil.getBanidoMotivo()).isEqualTo("spam");
    }

    @Test
    void visitanteComumNaoVeAvaliacoesOcultasNemDadosDeSuspensao() {
        when(albumRatingRepository.findVisiveisByUserId(anyLong(), any(Pageable.class))).thenReturn(Page.empty());

        communityService.listarAvaliacoes("joao", VISITANTE_ID, "recentes", 0, 20);
        PublicProfileResponse perfil = communityService.buscarPerfil("joao", VISITANTE_ID);

        verify(albumRatingRepository).findVisiveisByUserId(eq(DONO_ID), any(Pageable.class));
        verify(albumRatingRepository, never()).findByUserId(anyLong(), any(Pageable.class));
        assertThat(perfil.getBanido()).isNull();
        assertThat(perfil.isPodeModerar()).isFalse();
    }

    @Test
    void donoVeAsPropriasAvaliacoesOcultas() {
        when(albumRatingRepository.findByUserId(anyLong(), any(Pageable.class))).thenReturn(Page.empty());

        communityService.listarAvaliacoes("joao", DONO_ID, "recentes", 0, 20);

        verify(albumRatingRepository).findByUserId(eq(DONO_ID), any(Pageable.class));
    }

    @Test
    void playlistBloqueadaNaoAbreParaVisitante() {
        Playlist bloqueada = Playlist.builder().id("p1").userId(DONO_ID).publico(true).bloqueadaModeracao(true).build();
        when(playlistRepository.findByIdAndUserIdAndPublicoTrue("p1", DONO_ID)).thenReturn(Optional.of(bloqueada));

        assertThatThrownBy(() -> communityService.buscarPlaylist("joao", "p1", VISITANTE_ID))
                .isInstanceOf(PlaylistNotFoundException.class);
    }

    @Test
    void selosPublicosMostramSoPapeisElevados() {
        usuario.getRoles().add(com.synfonia.musicas.enums.RoleSistema.MODERATOR);

        assertThat(communityService.buscarPerfil("joao", VISITANTE_ID).getPapeis()).containsExactly("MODERATOR");
    }
}
