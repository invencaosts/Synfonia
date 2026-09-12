package com.synfonia.musicas.exceptions.records;

import java.time.LocalDateTime;
import java.util.Map;

public record ErrorResponse(
        int status,
        String titulo,
        String detalhe,
        LocalDateTime timestamp,
        String caminho,
        Map<String, String> erros
) {}
