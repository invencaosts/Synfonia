package com.synfonia.musicas.enums;

import java.util.EnumSet;
import java.util.Set;

import static com.synfonia.musicas.enums.Permissao.*;

/**
 * Papéis do sistema e suas permissões, definidos só em código. O banco guarda apenas quais papéis
 * cada usuário tem (tabela usuario_roles); USER é implícito para todos.
 *
 * {@code nivel} define a hierarquia: um usuário só pode agir sobre alvos de nível estritamente
 * menor que o seu e só pode conceder papéis de nível estritamente menor que o seu. Como nenhum
 * papel está acima de SUPER_ADMIN, ele só pode ser concedido pelo UsuariosSeeder (SEED_SUPER_ADMIN_* no .env).
 */
public enum RoleSistema {
    USER(0, "Usuário comum", EnumSet.noneOf(Permissao.class)),
    MODERATOR(10, "Modera conteúdo e suspende usuários comuns",
            EnumSet.of(PAINEL_MODERACAO_ACESSAR, USUARIOS_LER, USUARIOS_BANIR, CONTEUDO_MODERAR)),
    ADMIN(50, "Administra moderadores, vê dados de conta e auditoria",
            EnumSet.of(PAINEL_MODERACAO_ACESSAR, USUARIOS_LER, USUARIOS_DADOS_SENSIVEIS, USUARIOS_BANIR,
                    CONTEUDO_MODERAR, PAPEIS_GERENCIAR, AUDITORIA_LER)),
    SUPER_ADMIN(100, "Acesso total; só concedido via seed",
            EnumSet.allOf(Permissao.class));

    private final int nivel;
    private final String descricao;
    private final Set<Permissao> permissoes;

    RoleSistema(int nivel, String descricao, Set<Permissao> permissoes) {
        this.nivel = nivel;
        this.descricao = descricao;
        this.permissoes = permissoes;
    }

    public int getNivel() {
        return nivel;
    }

    public String getDescricao() {
        return descricao;
    }

    public Set<Permissao> getPermissoes() {
        return permissoes;
    }
}
