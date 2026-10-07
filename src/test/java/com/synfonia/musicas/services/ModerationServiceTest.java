package com.synfonia.musicas.services;

import com.synfonia.musicas.dtos.request.ModeracaoRequest;
import com.synfonia.musicas.entities.AlbumRating;
import com.synfonia.musicas.entities.AuditoriaModeracao;
import com.synfonia.musicas.entities.Usuario;
import com.synfonia.musicas.enums.Permissao;
import com.synfonia.musicas.enums.RoleSistema;
import com.synfonia.musicas.repositories.AlbumRatingRepository;
import com.synfonia.musicas.repositories.AuditoriaModeracaoRepository;
import com.synfonia.musicas.repositories.PlaylistRepository;
import com.synfonia.musicas.repositories.UsuarioRepository;
import com.synfonia.musicas.security.AccessPolicy;
import com.synfonia.musicas.security.UsuarioDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ModerationServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AlbumRatingRepository albumRatingRepository;
    @Mock private PlaylistRepository playlistRepository;
    @Mock private AuditoriaModeracaoRepository auditoriaRepository;
    @Mock private AlbumRatingService albumRatingService;

    private ModerationService service;
    private final Map<Long, Usuario> usuarios = new HashMap<>();

    private Usuario comum, outroComum, moderador, outroModerador, admin, outroAdmin, superAdmin;

    @BeforeEach
    void setUp() {
        service = new ModerationService(usuarioRepository, albumRatingRepository, playlistRepository,
                auditoriaRepository, albumRatingService, new AccessPolicy());

        comum = usuario(1L);
        outroComum = usuario(2L);
        moderador = usuario(10L, RoleSistema.MODERATOR);
        outroModerador = usuario(11L, RoleSistema.MODERATOR);
        admin = usuario(50L, RoleSistema.ADMIN);
        outroAdmin = usuario(51L, RoleSistema.ADMIN);
        superAdmin = usuario(100L, RoleSistema.SUPER_ADMIN);

        when(usuarioRepository.findById(any())).thenAnswer(inv -> Optional.ofNullable(usuarios.get(inv.<Long>getArgument(0))));
        when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(albumRatingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    private Usuario usuario(Long id, RoleSistema... roles) {
        Usuario u = Usuario.builder().id(id).username("u" + id).email("u" + id + "@x.com").senha("h").build();
        u.getRoles().addAll(Set.of(roles));
        usuarios.put(id, u);
        return u;
    }

    private void logarComo(Usuario u) {
        UsuarioDetails details = new UsuarioDetails(u);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    }

    private static ModeracaoRequest motivo() {
        return new ModeracaoRequest("Violou as regras da comunidade", null);
    }

    // ===== Authorities =====

    @Test
    void authoritiesVemDoPapelDefinidoNoCodigo() {
        UsuarioDetails details = new UsuarioDetails(moderador);
        assertThat(details.getAuthorities()).extracting("authority")
                .contains("ROLE_USER", "ROLE_MODERATOR", "USUARIOS_BANIR", "CONTEUDO_MODERAR")
                .doesNotContain("PAPEIS_GERENCIAR", "USUARIOS_DADOS_SENSIVEIS", "AUDITORIA_LER");
        assertThat(new UsuarioDetails(comum).getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
    }

    @Test
    void contaSuspensaFicaBloqueadaNoUserDetails() {
        comum.setBanido(true);
        assertThat(new UsuarioDetails(comum).isAccountNonLocked()).isFalse();
    }

    // ===== Suspensão =====

    @Test
    void moderadorSuspendeUsuarioComumEAudita() {
        logarComo(moderador);

        var resposta = service.suspender(comum.getId(), new ModeracaoRequest("Spam repetido", 7));

        assertThat(resposta.isBanido()).isTrue();
        assertThat(comum.getBanidoAte()).isNotNull();
        ArgumentCaptor<AuditoriaModeracao> captor = ArgumentCaptor.forClass(AuditoriaModeracao.class);
        verify(auditoriaRepository).save(captor.capture());
        assertThat(captor.getValue().getAcao()).isEqualTo("USUARIO_SUSPENDER");
        assertThat(captor.getValue().getAtorId()).isEqualTo(moderador.getId());
        assertThat(captor.getValue().getMotivo()).isEqualTo("Spam repetido");
    }

    @Test
    void moderadorNaoSuspendeAdmin() {
        logarComo(moderador);
        assertThatThrownBy(() -> service.suspender(admin.getId(), motivo())).isInstanceOf(AccessDeniedException.class);
        assertThat(admin.getBanido()).isFalse();
    }

    @Test
    void mesmoNivelNaoSeSuspendeEntreSi() {
        logarComo(moderador);
        assertThatThrownBy(() -> service.suspender(outroModerador.getId(), motivo())).isInstanceOf(AccessDeniedException.class);
        logarComo(admin);
        assertThatThrownBy(() -> service.suspender(outroAdmin.getId(), motivo())).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void ninguemSeSuspendeASiMesmo() {
        logarComo(superAdmin);
        assertThatThrownBy(() -> service.suspender(superAdmin.getId(), motivo())).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void superAdminNaoPodeSerSuspensoPorNinguem() {
        for (Usuario ator : List.of(moderador, admin)) {
            logarComo(ator);
            assertThatThrownBy(() -> service.suspender(superAdmin.getId(), motivo())).isInstanceOf(AccessDeniedException.class);
        }
    }

    @Test
    void usuarioComumNaoTemPermissaoDeSuspender() {
        logarComo(comum);
        assertThatThrownBy(() -> service.suspender(outroComum.getId(), motivo())).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void motivoEObrigatorio() {
        logarComo(moderador);
        assertThatThrownBy(() -> service.suspender(comum.getId(), new ModeracaoRequest("  ", null)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(auditoriaRepository, never()).save(any());
    }

    @Test
    void duracaoInvalidaERejeitada() {
        logarComo(moderador);
        assertThatThrownBy(() -> service.suspender(comum.getId(), new ModeracaoRequest("Spam repetido", 0)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ===== Papéis =====

    @Test
    void adminConcedeModerador() {
        logarComo(admin);
        service.concederRole(comum.getId(), "MODERATOR", motivo());
        assertThat(comum.getRoles()).containsExactly(RoleSistema.MODERATOR);
    }

    @Test
    void adminNaoConcedeAdminNemSuperAdmin() {
        logarComo(admin);
        assertThatThrownBy(() -> service.concederRole(comum.getId(), "ADMIN", motivo())).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.concederRole(comum.getId(), "SUPER_ADMIN", motivo())).isInstanceOf(AccessDeniedException.class);
        assertThat(comum.getRoles()).isEmpty();
    }

    @Test
    void superAdminNaoCriaOutroSuperAdminPelaApi() {
        logarComo(superAdmin);
        assertThatThrownBy(() -> service.concederRole(comum.getId(), "SUPER_ADMIN", motivo())).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void superAdminPromoveEDemoveAdmin() {
        logarComo(superAdmin);
        service.concederRole(comum.getId(), "ADMIN", motivo());
        assertThat(comum.getRoles()).contains(RoleSistema.ADMIN);
        service.removerRole(comum.getId(), "ADMIN", motivo());
        assertThat(comum.getRoles()).doesNotContain(RoleSistema.ADMIN);
    }

    @Test
    void adminNaoRebaixaSuperAdminNemOutroAdmin() {
        logarComo(admin);
        assertThatThrownBy(() -> service.removerRole(superAdmin.getId(), "MODERATOR", motivo())).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.removerRole(outroAdmin.getId(), "MODERATOR", motivo())).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void ninguemMexeNosPropriosPapeis() {
        logarComo(superAdmin);
        assertThatThrownBy(() -> service.concederRole(superAdmin.getId(), "ADMIN", motivo())).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void moderadorNaoGerenciaPapeis() {
        logarComo(moderador);
        assertThatThrownBy(() -> service.concederRole(comum.getId(), "MODERATOR", motivo())).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void papelUserNaoPodeSerRemovidoNemPapelInexistenteUsado() {
        logarComo(superAdmin);
        assertThatThrownBy(() -> service.removerRole(comum.getId(), "USER", motivo())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.concederRole(comum.getId(), "DEUS", motivo())).isInstanceOf(IllegalArgumentException.class);
    }

    // ===== Conteúdo =====

    @Test
    void moderadorOcultaAvaliacaoDeUsuarioComum() {
        AlbumRating rating = AlbumRating.builder().id("r1").userId(comum.getId()).albumName("A").artista("B").build();
        when(albumRatingRepository.findById("r1")).thenReturn(Optional.of(rating));
        logarComo(moderador);

        service.ocultarAvaliacao("r1", motivo());

        assertThat(rating.getOculto()).isTrue();
        assertThat(rating.getOcultoPor()).isEqualTo(moderador.getId());
    }

    @Test
    void moderadorNaoOcultaAvaliacaoDeAdmin() {
        AlbumRating rating = AlbumRating.builder().id("r2").userId(admin.getId()).build();
        when(albumRatingRepository.findById("r2")).thenReturn(Optional.of(rating));
        logarComo(moderador);

        assertThatThrownBy(() -> service.ocultarAvaliacao("r2", motivo())).isInstanceOf(AccessDeniedException.class);
        assertThat(rating.getOculto()).isNull();
    }

    // ===== Dados sensíveis =====

    @Test
    void emailSoAparecePraQuemTemDadosSensiveis() {
        when(usuarioRepository.buscarParaModeracao(anyString(), anyBoolean(), anyBoolean(), any()))
                .thenReturn(new PageImpl<>(List.of(comum)));

        logarComo(moderador);
        assertThat(service.buscarUsuarios("u1", false, 0, 20).getContent().get(0).getEmail()).isNull();

        logarComo(admin);
        assertThat(AccessPolicy.tem((UsuarioDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal(),
                Permissao.USUARIOS_DADOS_SENSIVEIS)).isTrue();
        assertThat(service.buscarUsuarios("u1", false, 0, 20).getContent().get(0).getEmail()).isEqualTo("u1@x.com");
    }

    @Test
    void painelInformaSoAsAcoesPermitidas() {
        when(usuarioRepository.buscarParaModeracao(anyString(), anyBoolean(), anyBoolean(), any()))
                .thenReturn(new PageImpl<>(List.of(comum, superAdmin)));
        logarComo(admin);

        var lista = service.buscarUsuarios("", false, 0, 20).getContent();

        assertThat(lista.get(0).isPodeSuspender()).isTrue();
        assertThat(lista.get(0).getRolesConcediveis()).containsExactly("MODERATOR");
        assertThat(lista.get(1).isPodeSuspender()).isFalse();
        assertThat(lista.get(1).getRolesConcediveis()).isEmpty();
    }
}
