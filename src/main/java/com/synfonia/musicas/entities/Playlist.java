package com.synfonia.musicas.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "playlists", indexes = {
        @Index(name = "idx_playlists_user_publico", columnList = "user_id, publico")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Playlist {

    // Texto: novos registros usam UUID; os migrados do Mongo mantêm o ObjectId (links continuam válidos)
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 64)
    private String id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // Só para a chave estrangeira com ON DELETE CASCADE: excluir a conta apaga as playlists
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_playlists_usuario"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Usuario usuario;

    @Column(columnDefinition = "TEXT")
    private String nome;

    @Column(length = 64)
    private String vibe;

    private boolean publico;

    @Column(columnDefinition = "TEXT")
    private String capaUrl;

    private boolean syncSpotify;

    @Column(length = 128)
    private String spotifyPlaylistId;

    // Ordem importa: a posição de cada faixa fica em playlist_tracks.posicao.
    // O ON DELETE CASCADE dessa FK vem da migration (Hibernate não aceita @OnDelete em @ElementCollection).
    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "playlist_tracks", joinColumns = @JoinColumn(name = "playlist_id",
            foreignKey = @ForeignKey(name = "fk_playlist_tracks_playlist")))
    @OrderColumn(name = "posicao")
    @Column(name = "track_id", length = 128, nullable = false)
    private List<String> trackIds = new ArrayList<>();

    // Moderação: playlist bloqueada não aparece na comunidade e o dono não consegue reexibi-la
    private Boolean bloqueadaModeracao;

    @Column(length = 500)
    private String bloqueioMotivo;
}
