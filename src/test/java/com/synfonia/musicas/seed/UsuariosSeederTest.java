package com.synfonia.musicas.seed;

import com.synfonia.musicas.entities.Usuario;
import com.synfonia.musicas.enums.RoleSistema;
import com.synfonia.musicas.repositories.AuditoriaModeracaoRepository;
import com.synfonia.musicas.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UsuariosSeederTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AuditoriaModeracaoRepository auditoriaRepository;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final Map<String, Usuario> banco = new HashMap<>();
    private MockEnvironment env;
    private UsuariosSeeder seeder;

    @BeforeEach
    void setUp() {
        env = new MockEnvironment();
        seeder = new UsuariosSeeder(usuarioRepository, auditoriaRepository, encoder, env);
        when(usuarioRepository.findByEmailIgnoreCase(anyString()))
                .thenAnswer(inv -> Optional.ofNullable(banco.get(inv.<String>getArgument(0).toLowerCase())));
        when(usuarioRepository.save(any())).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            if (u.getId() == null) u.setId((long) banco.size() + 1);
            banco.put(u.getEmail(), u);
            return u;
        });
    }

    @Test
    void criaAsTresContasComPapeisESenhaCriptografada() {
        env.setProperty("SEED_SUPER_ADMIN_EMAIL", "Root@Synfonia.local");
        env.setProperty("SEED_SUPER_ADMIN_PASSWORD", "Xk9#mPq2!vLw");
        env.setProperty("SEED_SUPER_ADMIN_USERNAME", "root");
        env.setProperty("SEED_MODERATOR_EMAIL", "mod@synfonia.local");
        env.setProperty("SEED_MODERATOR_PASSWORD", "Bz7$kRt4!pQx");
        env.setProperty("SEED_USER_EMAIL", "comum@synfonia.local");
        env.setProperty("SEED_USER_PASSWORD", "Hy3&wNv8!jKe");

        seeder.run(null);

        Usuario root = banco.get("root@synfonia.local");
        assertThat(root.getRoles()).containsExactly(RoleSistema.SUPER_ADMIN);
        assertThat(root.getUsername()).isEqualTo("root");
        assertThat(encoder.matches("Xk9#mPq2!vLw", root.getSenha())).isTrue();
        assertThat(banco.get("mod@synfonia.local").getRoles()).containsExactly(RoleSistema.MODERATOR);
        assertThat(banco.get("mod@synfonia.local").getUsername()).isEqualTo("mod");
        assertThat(banco.get("comum@synfonia.local").getRoles()).isEmpty();
    }

    @Test
    void contaExistenteNaoTemSenhaSobrescritaMasGanhaOPapel() {
        Usuario existente = Usuario.builder().id(7L).email("mod@synfonia.local").username("mod").senha("hash-antigo").build();
        banco.put(existente.getEmail(), existente);
        env.setProperty("SEED_MODERATOR_EMAIL", "mod@synfonia.local");
        env.setProperty("SEED_MODERATOR_PASSWORD", "Bz7$kRt4!pQx");

        seeder.run(null);
        seeder.run(null);

        assertThat(existente.getSenha()).isEqualTo("hash-antigo");
        assertThat(existente.getRoles()).containsExactly(RoleSistema.MODERATOR);
        verify(auditoriaRepository, org.mockito.Mockito.times(1)).save(any());
    }

    @Test
    void senhaFracaNaoCriaConta() {
        env.setProperty("SEED_SUPER_ADMIN_EMAIL", "root@synfonia.local");
        env.setProperty("SEED_SUPER_ADMIN_PASSWORD", "123456");

        seeder.run(null);

        assertThat(banco).isEmpty();
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void semVariaveisNaoCriaNada() {
        seeder.run(null);
        verify(usuarioRepository, never()).save(any());
    }
}
