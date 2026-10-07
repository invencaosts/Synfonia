package com.synfonia.musicas.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserResponse {
    private Long id;
    private String username;
    private String displayName;
    private String fotoPerfil;
    private List<String> roles;
    private int nivel;
    private boolean ativo;
    private boolean perfilPublico;
    private boolean banido;
    private LocalDateTime banidoAte;
    private String banidoMotivo;
    private LocalDateTime dataCriacao;

    // Só com USUARIOS_DADOS_SENSIVEIS
    private String email;
    private LocalDateTime ultimoLogin;

    // O que quem está vendo pode fazer com este usuário
    private boolean podeSuspender;
    private List<String> rolesConcediveis;
    private List<String> rolesRemoviveis;
}
