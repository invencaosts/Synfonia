package com.synfonia.musicas.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditoriaResponse {
    private Long id;
    private Long atorId;
    private String atorUsername;
    private String acao;
    private Long alvoUsuarioId;
    private String alvoUsername;
    private String alvoTipo;
    private String alvoId;
    private String motivo;
    private String detalhe;
    private LocalDateTime criadoEm;
}
