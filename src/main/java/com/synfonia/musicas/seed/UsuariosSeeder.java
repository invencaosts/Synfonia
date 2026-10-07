package com.synfonia.musicas.seed;

import com.synfonia.musicas.entities.AuditoriaModeracao;
import com.synfonia.musicas.entities.Usuario;
import com.synfonia.musicas.enums.RoleSistema;
import com.synfonia.musicas.repositories.AuditoriaModeracaoRepository;
import com.synfonia.musicas.repositories.UsuarioRepository;
import com.synfonia.musicas.services.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Seed das contas fixas: super admin, moderador e usuário comum. Credenciais vêm do ambiente (.env):
 *
 *   SEED_SUPER_ADMIN_EMAIL / SEED_SUPER_ADMIN_PASSWORD / SEED_SUPER_ADMIN_USERNAME
 *   SEED_MODERATOR_EMAIL   / SEED_MODERATOR_PASSWORD   / SEED_MODERATOR_USERNAME
 *   SEED_USER_EMAIL        / SEED_USER_PASSWORD        / SEED_USER_USERNAME
 *
 * Roda a cada inicialização e é idempotente:
 * - conta inexistente: é criada com a senha do .env (validada pelas mesmas regras do cadastro);
 * - conta existente: só garante o papel; a senha NÃO é sobrescrita (troca de senha não é desfeita a cada deploy);
 * - variáveis ausentes: a conta é ignorada (nada é criado com senha padrão).
 *
 * É o único caminho para criar um SUPER_ADMIN: pela API ninguém concede papel de nível igual ou maior que o próprio.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UsuariosSeeder implements ApplicationRunner {

    static final String ACAO_SEED_CONCEDER = "SEED_CONCEDER_PAPEL";
    static final String ACAO_SEED_CRIAR = "SEED_CRIAR_USUARIO";

    record ContaSeed(String prefixo, RoleSistema role, String displayName) {}

    static final List<ContaSeed> CONTAS = List.of(
            new ContaSeed("SEED_SUPER_ADMIN", RoleSistema.SUPER_ADMIN, "Super Admin"),
            new ContaSeed("SEED_MODERATOR", RoleSistema.MODERATOR, "Moderação"),
            new ContaSeed("SEED_USER", RoleSistema.USER, "Usuário Teste"));

    private final UsuarioRepository usuarioRepository;
    private final AuditoriaModeracaoRepository auditoriaRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (ContaSeed conta : CONTAS) {
            try {
                semear(conta);
            } catch (RuntimeException e) {
                // Uma conta mal configurada não derruba a aplicação nem as outras contas
                log.error("[UsuariosSeeder] {} ignorado: {}", conta.prefixo(), e.getMessage());
            }
        }

        long totalSuperAdmins = usuarioRepository.countByRole(RoleSistema.SUPER_ADMIN);
        if (totalSuperAdmins == 0) {
            log.warn("[UsuariosSeeder] Nenhum SUPER_ADMIN. Defina SEED_SUPER_ADMIN_EMAIL e SEED_SUPER_ADMIN_PASSWORD e reinicie.");
        }
    }

    void semear(ContaSeed conta) {
        String email = valor(conta.prefixo() + "_EMAIL");
        String senha = valor(conta.prefixo() + "_PASSWORD");
        if (email == null || senha == null) {
            log.info("[UsuariosSeeder] {} não configurado, pulando", conta.prefixo());
            return;
        }
        email = email.toLowerCase();

        var existente = usuarioRepository.findByEmailIgnoreCase(email);
        Usuario usuario;
        if (existente.isPresent()) {
            usuario = existente.get();
        } else {
            AuthService.validatePassword(senha, email);
            String username = valor(conta.prefixo() + "_USERNAME");
            if (username == null) username = email.substring(0, email.indexOf('@')).replaceAll("[^a-zA-Z0-9_]", "_");
            if (usuarioRepository.existsByUsername(username)) {
                throw new IllegalStateException("username " + username + " já está em uso por outra conta");
            }

            usuario = Usuario.builder()
                    .email(email)
                    .senha(passwordEncoder.encode(senha))
                    .username(username)
                    .displayName(conta.displayName())
                    .ativo(true)
                    .build();
            usuario = usuarioRepository.save(usuario);
            auditar(ACAO_SEED_CRIAR, usuario, conta.role().name());
            log.warn("[UsuariosSeeder] Conta {} criada: id={} @{}", conta.role(), usuario.getId(), usuario.getUsername());
        }

        if (conta.role() != RoleSistema.USER && usuario.getRoles().add(conta.role())) {
            usuarioRepository.save(usuario);
            auditar(ACAO_SEED_CONCEDER, usuario, conta.role().name());
            log.warn("[UsuariosSeeder] Papel {} garantido para id={} @{}", conta.role(), usuario.getId(), usuario.getUsername());
        }
    }

    private void auditar(String acao, Usuario usuario, String detalhe) {
        auditoriaRepository.save(AuditoriaModeracao.builder()
                .acao(acao)
                .alvoUsuarioId(usuario.getId())
                .alvoTipo("USUARIO")
                .alvoId(String.valueOf(usuario.getId()))
                .detalhe(detalhe)
                .build());
    }

    private String valor(String chave) {
        String v = environment.getProperty(chave);
        return v == null || v.isBlank() ? null : v.trim();
    }
}
