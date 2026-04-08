package com.joaopaulo.musicas.entities;

import jakarta.persistence.*;
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

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank
    private String senha; // Sempre armazenada como hash BCrypt

    @Column(unique = true, nullable = false)
    private String username;

    // Nick Customizável (H1) - Aceita letras, números e símbolos (sem emojis)
    private String displayName;

    // Nome Pessoal (Subtítulo) - Apenas letras e espaços
    private String personalName;

    @Builder.Default
    @Column(columnDefinition = "boolean default true")
    private Boolean showPersonalName = true;

    @Builder.Default
    @Column(columnDefinition = "boolean default true")
    private Boolean showSpotifyActivity = true;

    private LocalDateTime dataDesativacao;

    @NotNull
    @Builder.Default
    private Papel papel = Papel.USER;

    @Builder.Default
    private boolean ativo = true;


    private LocalDateTime ultimoLogin;

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

    @Builder.Default
    @Column(name = "username_changed")
    private boolean usernameChanged = false;

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