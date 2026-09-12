package com.synfonia.musicas.controllers;

import com.synfonia.musicas.dtos.response.HistoricoResponse;
import com.synfonia.musicas.security.UsuarioDetails;
import com.synfonia.musicas.services.HistoricoReproducaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/historico")
@RequiredArgsConstructor
@Tag(name = "Histórico de Reprodução", description = "Endpoints para registro e visualização de músicas ouvidas")
public class HistoricoReproducaoController {

    private final HistoricoReproducaoService historicoService;

    @Operation(summary = "Registra que o usuário ouviu uma música")
    @PostMapping("/{trackId}")
    public ResponseEntity<Void> adicionarAoHistorico(
            @AuthenticationPrincipal UsuarioDetails userDetails,
            @PathVariable String trackId,
            @RequestBody(required = false) com.synfonia.musicas.dtos.request.MusicSaveRequest request) {
        
        historicoService.adicionarAoHistorico(userDetails.getId(), trackId, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Retorna as últimas músicas ouvidas pelo usuário")
    @GetMapping
    public ResponseEntity<List<HistoricoResponse>> obterHistorico(
            @AuthenticationPrincipal UsuarioDetails userDetails) {
        
        List<HistoricoResponse> historico = historicoService.obterHistoricoRecente(userDetails.getId());
        return ResponseEntity.ok(historico);
    }
}
