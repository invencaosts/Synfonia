package com.synfonia.musicas.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.synfonia.musicas.enums.RoleSistema;
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
    @Builder.Default
    @Column(name = "nome_completo", nullable = false)
    private String personalName = "";

    @Builder.Default
    @Column(columnDefinition = "boolean default true")
    private Boolean showPersonalName = true;

    @Builder.Default
    @Column(columnDefinition = "boolean default true")
    private Boolean showSpotifyActivity = true;

    // Privacidade da comunidade: perfil visível na busca e abas visíveis para outros usuários
    @Builder.Default
    @Column(name = "perfil_publico", columnDefinition = "boolean default true")
    private Boolean perfilPublico = true;

    @Builder.Default
    @Column(name = "show_curtidas", columnDefinition = "boolean default true")
    private Boolean showCurtidas = true;

    @Builder.Default
    @Column(name = "show_avaliacoes", columnDefinition = "boolean default true")
    private Boolean showAvaliacoes = true;

    private LocalDateTime dataDesativacao;

    /**
     * @deprecated Legado. Autorização usa {@link #roles}; a coluna fica só por compatibilidade de schema
     * e NÃO concede nenhum acesso (podia ser obtida pela antiga promoção automática no cadastro).
     */
    @Deprecated
    @NotNull
    @Builder.Default
    private Papel papel = Papel.USER;

    // Papéis elevados do usuário. USER é implícito para todos e não é gravado.
    // As definições (nível e permissões) ficam no código: RoleSistema/Permissao.
    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "usuario_roles", joinColumns = @JoinColumn(name = "usuario_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 32, nullable = false)
    private java.util.Set<RoleSistema> roles = new java.util.HashSet<>();

    // Suspensão pela moderação (diferente de ativo=false, que é a desativação pelo próprio usuário
    // e leva à exclusão automática da conta). banidoAte null + banido = suspensão permanente.
    @Builder.Default
    @Column(name = "banido", columnDefinition = "boolean default false")
    private Boolean banido = false;

    @Column(name = "banido_ate")
    private LocalDateTime banidoAte;

    @Column(name = "banido_motivo", length = 500)
    private String banidoMotivo;

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

    private String instagramLink;
    private String spotifyLink;
    private String youtubeLink;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private com.synfonia.musicas.enums.MusicSource preferredMusicSource = com.synfonia.musicas.enums.MusicSource.ITUNES;
    
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

    public boolean isBanidoAgora() {
        return Boolean.TRUE.equals(banido) && (banidoAte == null || banidoAte.isAfter(LocalDateTime.now()));
    }

    public int getNivelMaximo() {
        return getRolesEfetivas().stream().mapToInt(RoleSistema::getNivel).max().orElse(0);
    }

    /** Papéis gravados + USER implícito. */
    public java.util.Set<RoleSistema> getRolesEfetivas() {
        java.util.EnumSet<RoleSistema> efetivas = java.util.EnumSet.of(RoleSistema.USER);
        if (roles != null) efetivas.addAll(roles);
        return efetivas;
    }

    public java.util.Set<String> getNomesRoles() {
        return getRolesEfetivas().stream().map(Enum::name)
                .collect(java.util.stream.Collectors.toCollection(java.util.TreeSet::new));
    }

    public java.util.Set<String> getCodigosPermissoes() {
        return getRolesEfetivas().stream()
                .flatMap(r -> r.getPermissoes().stream())
                .map(Enum::name)
                .collect(java.util.stream.Collectors.toCollection(java.util.TreeSet::new));
    }

    public enum Papel {
        ADMIN, USER
    }
}