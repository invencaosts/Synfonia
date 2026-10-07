package com.synfonia.musicas.dtos.response;

import java.util.List;

public record RoleResponse(String nome, String descricao, int nivel, List<String> permissoes, boolean concedivel) {}
