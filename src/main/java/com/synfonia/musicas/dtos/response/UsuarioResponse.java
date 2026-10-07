package com.synfonia.musicas.dtos.response;

import com.synfonia.musicas.entities.Usuario;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioResponse {
    private Long id;
    private String email;
    private String username;
    private String displayName;
    private String personalName;
    private boolean showPersonalName;
    private boolean showSpotifyActivity;
    private boolean perfilPublico;
    private boolean showCurtidas;
    private boolean showAvaliacoes;
    private java.time.LocalDateTime dataDesativacao;
    // RBAC: papéis efetivos (inclui USER) e permissões, para o front decidir o que exibir.
    // A autorização real é sempre no backend.
    private java.util.List<String> roles;
    private java.util.List<String> permissoes;
    private boolean ativo;
    private java.time.LocalDateTime dataCriacao;
    private java.time.LocalDateTime ultimoLogin;
    
    private String favoriteTrackId;
    private String favoriteTrackName;
    private String favoriteTrackArtist;
    private String favoriteTrackCapaUrl;
    private String favoriteTrackPreviewUrl;
    
    private String fotoPerfil;
    private boolean usernameChanged;

    private java.util.Map<String, String> socialLinks;
    private com.synfonia.musicas.enums.MusicSource preferredMusicSource;
}