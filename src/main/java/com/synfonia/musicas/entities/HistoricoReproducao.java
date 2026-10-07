package com.synfonia.musicas.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/**
 * Uma linha por (usuário, faixa): tocar de novo só atualiza {@code dataReproducao}.
 */
@Entity
@Table(name = "historico_reproducao",
        uniqueConstraints = @UniqueConstraint(name = "uk_historico_usuario_faixa", columnNames = {"user_id", "track_id"}),
        indexes = @Index(name = "idx_historico_usuario_data", columnList = "user_id, data_reproducao"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoricoReproducao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 64)
    private String id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_historico_usuario"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Usuario usuario;

    @Column(name = "track_id", length = 128, nullable = false)
    private String trackId;

    @Column(name = "data_reproducao")
    private LocalDateTime dataReproducao;
}
