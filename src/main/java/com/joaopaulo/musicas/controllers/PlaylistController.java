package com.joaopaulo.musicas.controllers;

import com.joaopaulo.musicas.dtos.request.MusicSaveRequest;
import com.joaopaulo.musicas.dtos.request.PlaylistRequest;
import com.joaopaulo.musicas.dtos.request.SpotifyImportDataRequest;
import com.joaopaulo.musicas.entities.Playlist;
import com.joaopaulo.musicas.services.PlaylistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/playlists")
@RequiredArgsConstructor
@Tag(name = "Playlists", description = "Endpoints para gerenciamento de playlists dos usuários")
public class PlaylistController {

    private final PlaylistService playlistService;

    @Operation(summary = "Cria uma nova playlist", description = "Cria uma playlist com nome, vibe, privacidade e capa (Base64).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Playlist criada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da playlist inválidos"),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado"),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })
    @PostMapping
    public ResponseEntity<Playlist> create(@RequestBody PlaylistRequest request) {
        Long userId = playlistService.getLoggedUserId();
        Playlist playlist = Playlist.builder()
                .nome(request.getNome())
                .vibe(request.getVibe())
                .publico(request.isPublico())
                .capaUrl(request.getCapaUrl())
                .userId(userId)
                .trackIds(new java.util.ArrayList<>())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(playlistService.save(playlist));
    }

    @Operation(summary = "Atualizar uma playlist existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Playlist atualizada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autorizado"),
            @ApiResponse(responseCode = "404", description = "Playlist não encontrada")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Playlist> update(
            @PathVariable String id,
            @RequestBody PlaylistRequest request) {
        Playlist playlistDetails = Playlist.builder()
                .nome(request.getNome())
                .vibe(request.getVibe())
                .publico(request.isPublico())
                .capaUrl(request.getCapaUrl())
                .build();
        return ResponseEntity.ok(playlistService.update(id, playlistDetails));
    }

    @Operation(summary = "Listar todas as playlists do usuário logado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Playlists recuperadas com sucesso"),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado"),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })

    @GetMapping
    public ResponseEntity<List<Playlist>> getAll() {
        Long userId = playlistService.getLoggedUserId();
        return ResponseEntity.ok(playlistService.findAllByUserId(userId));
    }

    @Operation(summary = "Listar playlists públicas de um perfil específico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Playlists públicas recuperadas com sucesso"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado"),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })

    @GetMapping("/public/{userId}")
    public ResponseEntity<List<Playlist>> getPublic(
            @Parameter(description = "ID do usuário dono do perfil") @PathVariable Long userId) {
        return ResponseEntity.ok(playlistService.findPublicByUserId(userId));
    }

    @Operation(summary = "Adicionar uma música à playlist")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Música adicionada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Música já existe na playlist ou IDs inválidos"),
            @ApiResponse(responseCode = "401", description = "Não autorizado"),
            @ApiResponse(responseCode = "404", description = "Playlist não encontrada"),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })

    @PostMapping("/{playlistId}/tracks")
    public ResponseEntity<Playlist> addTrack(
            @Parameter(description = "ID da playlist (MongoDB)") @PathVariable String playlistId,
            @RequestBody MusicSaveRequest request) {
        return ResponseEntity.ok(playlistService.addTrack(playlistId, request));
    }

    @Operation(summary = "Adicionar várias músicas à playlist (Batch)")
    @PostMapping("/{playlistId}/tracks/batch")
    public ResponseEntity<Playlist> addTracks(
            @Parameter(description = "ID da playlist (MongoDB)") @PathVariable String playlistId,
            @RequestBody List<MusicSaveRequest> requests) {
        return ResponseEntity.ok(playlistService.addTracks(playlistId, requests));
    }

    @Operation(summary = "Importa uma playlist do Spotify enviando os dados diretamente do frontend (Bypass 403)")
    @PostMapping("/import-data")
    public ResponseEntity<Playlist> importSpotifyData(
            @RequestBody SpotifyImportDataRequest request) {
        return ResponseEntity.ok(playlistService.importPlaylistData(request));
    }

    @Operation(summary = "Remover uma música da playlist")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Música removida com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autorizado"),
            @ApiResponse(responseCode = "404", description = "Playlist ou música não encontrada"),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })

    @DeleteMapping("/{playlistId}/tracks/{trackId}")
    public ResponseEntity<Playlist> removeTrack(
            @Parameter(description = "ID da playlist (MongoDB)") @PathVariable String playlistId,
            @Parameter(description = "ID da música") @PathVariable String trackId) {
        return ResponseEntity.ok(playlistService.removeTrack(playlistId, trackId));
    }

    @Operation(summary = "Excluir permanentemente uma playlist")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Playlist excluída com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autorizado"),
            @ApiResponse(responseCode = "404", description = "Playlist não encontrada"),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID da playlist") @PathVariable String id) {
        playlistService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
