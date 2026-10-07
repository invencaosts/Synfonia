package com.synfonia.musicas.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Perfil de um usuário visto por outra pessoa da comunidade.
 * Campos privados (email, papel, login) nunca entram aqui; contadores de abas ocultas vêm nulos.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicProfileResponse {
    private Long id;
    private String username;
    private String displayName;
    private String personalName;
    private String fotoPerfil;
    private LocalDateTime dataCriacao;
    private Map<String, String> socialLinks;

    private String favoriteTrackId;
    private String favoriteTrackName;
    private String favoriteTrackArtist;
    private String favoriteTrackCapaUrl;
    private String favoriteTrackPreviewUrl;

    private boolean proprioPerfil;
    private boolean avaliacoesVisiveis;
    private boolean curtidasVisiveis;

    private Long totalAvaliacoes;
    private Double mediaAvaliacoes;
    private Long totalPlaylistsPublicas;
    private Long totalCurtidas;

    // Papéis acima de USER, exibidos como selo público (ex.: MODERATOR)
    private java.util.List<String> papeis;

    // Visão de moderação: preenchidos só para quem tem permissão
    private boolean podeModerar;
    private Boolean banido;
    private LocalDateTime banidoAte;
    private String banidoMotivo;
}
