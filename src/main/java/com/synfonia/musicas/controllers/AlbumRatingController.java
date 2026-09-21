package com.synfonia.musicas.controllers;

import com.synfonia.musicas.dtos.request.AlbumRatingRequest;
import com.synfonia.musicas.dtos.response.AlbumRatingResponse;
import com.synfonia.musicas.security.UsuarioDetails;
import com.synfonia.musicas.services.AlbumRatingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/avaliacoes-album")
@RequiredArgsConstructor
@Tag(name = "Avaliações de Álbum", description = "Endpoints para avaliar álbuns com nota e resenha")
public class AlbumRatingController {

    private final AlbumRatingService albumRatingService;

    @Operation(summary = "Cria ou atualiza a avaliação do usuário para um álbum")
    @PostMapping
    public ResponseEntity<AlbumRatingResponse> salvar(
            @AuthenticationPrincipal UsuarioDetails userDetails,
            @Valid @RequestBody AlbumRatingRequest request) {

        return ResponseEntity.ok(albumRatingService.salvar(userDetails.getId(), request));
    }

    @Operation(summary = "Busca a avaliação do usuário atual para um álbum específico")
    @GetMapping("/{albumKey}")
    public ResponseEntity<AlbumRatingResponse> buscar(
            @AuthenticationPrincipal UsuarioDetails userDetails,
            @PathVariable String albumKey) {

        return ResponseEntity.ok(albumRatingService.buscar(userDetails.getId(), albumKey));
    }

    @Operation(summary = "Lista todas as avaliações de álbum do usuário atual")
    @GetMapping
    public ResponseEntity<List<AlbumRatingResponse>> listar(
            @AuthenticationPrincipal UsuarioDetails userDetails) {

        return ResponseEntity.ok(albumRatingService.listar(userDetails.getId()));
    }

    @Operation(summary = "Exclui a avaliação do usuário atual para um álbum")
    @DeleteMapping("/{albumKey}")
    public ResponseEntity<Void> excluir(
            @AuthenticationPrincipal UsuarioDetails userDetails,
            @PathVariable String albumKey) {

        albumRatingService.excluir(userDetails.getId(), albumKey);
        return ResponseEntity.noContent().build();
    }
}
