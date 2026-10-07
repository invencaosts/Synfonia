package com.synfonia.musicas.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Registro imutável de cada ação de moderação/administração. Nunca é atualizado nem apagado pela API.
 */
@Entity
@Table(name = "auditoria_moderacao", indexes = {
        @Index(name = "idx_auditoria_criado_em", columnList = "criado_em"),
        @Index(name = "idx_auditoria_alvo_usuario", columnList = "alvo_usuario_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditoriaModeracao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Null quando a ação vem do seed (sistema)
    @Column(name = "ator_id")
    private Long atorId;

    @Column(nullable = false, length = 64)
    private String acao;

    @Column(name = "alvo_usuario_id")
    private Long alvoUsuarioId;

    @Column(name = "alvo_tipo", length = 32)
    private String alvoTipo;

    @Column(name = "alvo_id", length = 64)
    private String alvoId;

    @Column(length = 500)
    private String motivo;

    @Column(columnDefinition = "TEXT")
    private String detalhe;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    protected void onCreate() {
        if (criadoEm == null) criadoEm = LocalDateTime.now();
    }
}
