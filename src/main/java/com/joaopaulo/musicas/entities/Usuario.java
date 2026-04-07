package com.joaopaulo.musicas.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios", indexes = {
    @Index(name = "uk_email", columnList = "email", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Email
    @NotBlank
    @Size(max = 255)
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank
    private String senha; // Sempre armazenada como hash BCrypt

    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, unique = true)
    private String username;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false)
    private String displayName;

    @Size(max = 100)
    @Column(name = "nome_completo")
    private String personalName;

    @Builder.Default
    private boolean showPersonalName = true;

    @Builder.Default
    private boolean showSpotifyActivity = true;

    @NotNull
    @Builder.Default
    private Papel papel = Papel.USER;

    @Builder.Default
    private boolean ativo = true;

    @Builder.Default
    private boolean usernameChanged = false;

    private LocalDateTime ultimoLogin;
    private LocalDateTime dataDesativacao;

    @Builder.Default
    private Integer tentativasFalhas = 0;
    private LocalDateTime bloqueadoAte;
    
    private String favoriteTrackId;
    private String favoriteTrackName;
    private String favoriteTrackArtist;
    private String favoriteTrackCapaUrl;
    private String favoriteTrackPreviewUrl;
    
    @Column(columnDefinition = "TEXT")
    private String fotoPerfil;

    @Column(updatable = false)
    private LocalDateTime dataCriacao;

    @PrePersist
    protected void onCreate() {
        dataCriacao = LocalDateTime.now();
    }

    public enum Papel {
        ADMIN, USER
    }
}