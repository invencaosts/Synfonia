package com.synfonia.musicas.enums;

/**
 * Permissões atômicas do RBAC, definidas só em código. O nome do enum é a authority usada em
 * {@code @PreAuthorize("hasAuthority('...')")}. Para criar uma, adicione aqui e associe em {@link RoleSistema}.
 */
public enum Permissao {
    PAINEL_MODERACAO_ACESSAR("Acessar o painel de moderação"),
    USUARIOS_LER("Listar e consultar usuários no painel"),
    USUARIOS_DADOS_SENSIVEIS("Ver e-mail e dados de conta dos usuários"),
    USUARIOS_BANIR("Suspender e reativar usuários"),
    CONTEUDO_MODERAR("Ocultar e reexibir avaliações e playlists públicas"),
    PAPEIS_GERENCIAR("Conceder e remover papéis de usuários"),
    AUDITORIA_LER("Consultar o log de auditoria da moderação");

    private final String descricao;

    Permissao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
