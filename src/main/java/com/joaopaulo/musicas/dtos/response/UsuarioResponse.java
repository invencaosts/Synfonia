package com.joaopaulo.musicas.dtos.response;

import com.joaopaulo.musicas.entities.Usuario;
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
    private java.time.LocalDateTime dataDesativacao;
    private Usuario.Papel papel;
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
}