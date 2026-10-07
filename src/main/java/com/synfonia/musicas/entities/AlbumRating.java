package com.synfonia.musicas.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.synfonia.musicas.enums.MusicSource;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "album_ratings",
        uniqueConstraints = @UniqueConstraint(name = "uk_album_ratings_usuario_album", columnNames = {"user_id", "album_key"}),
        indexes = @Index(name = "idx_album_ratings_usuario_data", columnList = "user_id, atualizado_em"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlbumRating {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 64)
    private String id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_album_ratings_usuario"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Usuario usuario;

    @Column(name = "album_key", length = 512, nullable = false)
    private String albumKey;

    @Column(columnDefinition = "TEXT")
    private String albumName;

    @Column(columnDefinition = "TEXT")
    private String artista;

    @Column(columnDefinition = "TEXT")
    private String capaUrl;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private MusicSource source;

    private Double nota;

    @Column(length = 200)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String review;

    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    // Moderação: avaliação oculta some da comunidade, mas continua visível para o dono e moderadores
    private Boolean oculto;

    @Column(length = 500)
    private String ocultoMotivo;

    private Long ocultoPor;

    private LocalDateTime ocultoEm;
}
