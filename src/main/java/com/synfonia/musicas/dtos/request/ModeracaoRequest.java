package com.synfonia.musicas.dtos.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModeracaoRequest {
    /** Obrigatório em toda ação de moderação (5 a 500 caracteres). */
    private String motivo;
    /** Suspensão: duração em dias (1 a 3650). Nulo = permanente. */
    private Integer dias;
}
