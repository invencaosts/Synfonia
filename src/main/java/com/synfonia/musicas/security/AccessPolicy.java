package com.synfonia.musicas.security;

import com.synfonia.musicas.entities.Usuario;
import com.synfonia.musicas.enums.Permissao;
import com.synfonia.musicas.enums.RoleSistema;
import com.synfonia.musicas.exceptions.UnauthorizedException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Regras de hierarquia do RBAC, aplicadas além do {@code @PreAuthorize} de cada endpoint:
 *
 * - ninguém age sobre a própria conta (não se suspende, não mexe nos próprios papéis);
 * - só se age sobre quem tem nível estritamente menor (moderador não toca em admin; admin não toca em admin);
 * - só se concede/remove papel de nível estritamente menor que o próprio (logo, SUPER_ADMIN nunca pela API).
 */
@Component
public class AccessPolicy {

    public Optional<UsuarioDetails> atualOpcional() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UsuarioDetails details) {
            return Optional.of(details);
        }
        return Optional.empty();
    }

    public UsuarioDetails atual() {
        return atualOpcional().orElseThrow(() -> new UnauthorizedException("Usuário não está autenticado"));
    }

    public boolean tem(Permissao permissao) {
        return atualOpcional().map(d -> tem(d, permissao)).orElse(false);
    }

    public static boolean tem(UsuarioDetails ator, Permissao permissao) {
        return ator.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(permissao.name()::equals);
    }

    public void exigir(UsuarioDetails ator, Permissao permissao) {
        if (!tem(ator, permissao)) {
            throw new AccessDeniedException("Permissão necessária: " + permissao.name());
        }
    }

    /** true se o ator pode agir sobre o alvo (não é ele mesmo e tem nível estritamente maior). */
    public static boolean podeAgirSobre(UsuarioDetails ator, Usuario alvo) {
        return !ator.getId().equals(alvo.getId()) && ator.getNivel() > alvo.getNivelMaximo();
    }

    public void exigirSuperioridade(UsuarioDetails ator, Usuario alvo) {
        if (ator.getId().equals(alvo.getId())) {
            throw new AccessDeniedException("Você não pode executar esta ação na sua própria conta.");
        }
        if (ator.getNivel() <= alvo.getNivelMaximo()) {
            throw new AccessDeniedException("Você não pode agir sobre uma conta de nível igual ou superior ao seu.");
        }
    }

    public static boolean podeGerenciarRole(UsuarioDetails ator, RoleSistema role) {
        return role != RoleSistema.USER && ator.getNivel() > role.getNivel();
    }

    public void exigirPodeGerenciarRole(UsuarioDetails ator, RoleSistema role) {
        if (role == RoleSistema.USER) {
            throw new IllegalArgumentException("O papel USER é implícito e não pode ser concedido nem removido.");
        }
        if (ator.getNivel() <= role.getNivel()) {
            throw new AccessDeniedException("Você não pode conceder ou remover o papel " + role.name() + ".");
        }
    }
}
