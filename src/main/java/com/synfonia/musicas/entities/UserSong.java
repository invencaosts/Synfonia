package com.synfonia.musicas.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.synfonia.musicas.enums.MusicSource;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_songs",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_songs_usuario_faixa", columnNames = {"user_id", "track_id"}),
        indexes = @Index(name = "idx_user_songs_usuario_data", columnList = "user_id, data_adicao"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSong {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 64)
    private String id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_user_songs_usuario"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Usuario usuario;

    // ID da faixa (iTunes numérico, YouTube/Spotify alfanumérico)
    @Column(name = "track_id", length = 128, nullable = false)
    private String trackId;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private MusicSource source;

    // Cópia dos metadados para busca/ordenação da biblioteca sem join com o catálogo
    @Column(columnDefinition = "TEXT")
    private String trackName;

    @Column(columnDefinition = "TEXT")
    private String artistName;

    @Column(columnDefinition = "TEXT")
    private String albumName;

    @Column(name = "data_adicao")
    private LocalDateTime dataAdicao;
}
