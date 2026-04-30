package com.joaopaulo.musicas.controllers;

import com.joaopaulo.musicas.dtos.response.MusicResponse;
import com.joaopaulo.musicas.mappers.MusicMapper;
import com.joaopaulo.musicas.services.MusicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/musicas")
@RequiredArgsConstructor
@Tag(name = "Músicas", description = "Endpoints para busca e gerenciamento do catálogo musical")
public class MusicController {

    private final MusicService musicService;
    private final MusicMapper musicMapper;

    @Operation(summary = "Busca músicas com filtros opcionais",
            description = "O nome é obrigatório. Artista e Álbum ajudam a refinar a busca na Apple Music.")
    @GetMapping("/search")
    public ResponseEntity<List<MusicResponse>> search(
            @RequestParam("nome") String nome,
            @RequestParam(value = "artista", required = false) String artista,
            @RequestParam(value = "album", required = false) String album,
            @RequestParam(value = "tipo", defaultValue = "all") String tipo) {

        var entities = musicService.searchByFilter(nome, artista, album, tipo, 100);
        return ResponseEntity.ok(musicMapper.toResponseList(entities));
    }

    @Operation(summary = "Busca uma música específica pelo ID interno")
    @GetMapping("/{id}")
    public ResponseEntity<MusicResponse> getById(@PathVariable("id") String id) {
        var entity = musicService.findById(id);
        return ResponseEntity.ok(musicMapper.toResponse(entity));
    }

    @Operation(summary = "Busca múltiplas músicas pelos IDs internos (Batch)")
    @PostMapping("/batch")
    public ResponseEntity<List<MusicResponse>> getByIds(@RequestBody List<String> ids) {
        var entities = musicService.findAllByIds(ids);
        return ResponseEntity.ok(musicMapper.toResponseList(entities));
    }
}
