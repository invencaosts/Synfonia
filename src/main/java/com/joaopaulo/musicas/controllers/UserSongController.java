package com.joaopaulo.musicas.controllers;

import com.joaopaulo.musicas.entities.UserSong;
import com.joaopaulo.musicas.enums.MusicSource;
import com.joaopaulo.musicas.services.UserSongService;

import com.joaopaulo.musicas.security.UsuarioDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Gerenciamento de Músicas Salvas", description = "Endpoints para adicionar e listar músicas salvas no perfil")
public class UserSongController {

    private final UserSongService userSongService;

    @Operation(summary = "Listar as músicas do usuário atual")
    @GetMapping("/me/songs")
    public ResponseEntity<Page<com.joaopaulo.musicas.dtos.response.UserSongResponse>> listarMusicasMe(
            @AuthenticationPrincipal UsuarioDetails details,
            @RequestParam(value = "q", required = false) String searchTerm,
            @PageableDefault(size = 50, sort = "dataAdicao", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<com.joaopaulo.musicas.dtos.response.UserSongResponse> musicas = userSongService.listarMusicas(details.getId(), searchTerm, pageable);
        return ResponseEntity.ok(musicas);
    }

    @Operation(summary = "Listar apenas os IDs das músicas curtidas pelo usuário atual")
    @GetMapping("/me/songs/ids")
    public ResponseEntity<java.util.List<String>> listarIdsMusicasMe(@AuthenticationPrincipal UsuarioDetails details) {
        java.util.List<String> ids = userSongService.listarIdsMusicasCurtidas(details.getId());
        return ResponseEntity.ok(ids);
    }
    
    @Operation(summary = "Adicionar música ao perfil do usuário atual")
    @PostMapping("/me/songs")
    public ResponseEntity<UserSong> adicionarMusicaMe(@AuthenticationPrincipal UsuarioDetails details, @RequestBody com.joaopaulo.musicas.dtos.request.MusicSaveRequest request) {
        UserSong userSong = userSongService.adicionarMusica(details.getId(), request);
        return ResponseEntity.ok(userSong);
    }

    @Operation(summary = "Remover música do perfil do usuário atual")
    @DeleteMapping("/me/songs/{trackId}")
    public ResponseEntity<Void> removerMusicaMe(@AuthenticationPrincipal UsuarioDetails details, @PathVariable("trackId") String trackId) {
        userSongService.removerMusica(details.getId(), trackId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Verificar se uma música específica está salva no perfil do usuário atual")
    @GetMapping("/me/songs/{trackId}")
    public ResponseEntity<UserSong> verificarMusicaMe(@AuthenticationPrincipal UsuarioDetails details, @PathVariable("trackId") String trackId) {
        return userSongService.verificarMusicaSalva(details.getId(), trackId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Remover músicas do perfil atual por fonte (Ex: SPOTIFY)")
    @DeleteMapping("/me/songs/source/{source}")
    public ResponseEntity<Long> removerMusicasPorFonteMe(@AuthenticationPrincipal UsuarioDetails details, @PathVariable("source") MusicSource source) {
        Long count = userSongService.removerMusicasPorFonte(details.getId(), source);
        return ResponseEntity.ok(count);
    }

    @Operation(summary = "Adicionar música ao perfil do usuário")
    @PostMapping("/{userId}/songs")
    public ResponseEntity<UserSong> adicionarMusica(@PathVariable("userId") Long userId, @RequestBody com.joaopaulo.musicas.dtos.request.MusicSaveRequest request) {
        UserSong userSong = userSongService.adicionarMusica(userId, request);
        return ResponseEntity.ok(userSong);
    }

    @Operation(summary = "Remover música do perfil do usuário")
    @DeleteMapping("/{userId}/songs/{trackId}")
    public ResponseEntity<Void> removerMusica(@PathVariable("userId") Long userId, @PathVariable("trackId") String trackId) {
        userSongService.removerMusica(userId, trackId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Remover músicas do perfil por fonte (Ex: SPOTIFY)")
    @DeleteMapping("/{userId}/songs/source/{source}")
    public ResponseEntity<Long> removerMusicasPorFonte(@PathVariable("userId") Long userId, @PathVariable("source") MusicSource source) {
        Long count = userSongService.removerMusicasPorFonte(userId, source);
        return ResponseEntity.ok(count);
    }

    @Operation(summary = "Listar todas as músicas salvas por um usuário")
    @GetMapping("/{userId}/songs")
    public ResponseEntity<Page<com.joaopaulo.musicas.dtos.response.UserSongResponse>> listarMusicas(
            @PathVariable("userId") Long userId,
            @RequestParam(value = "q", required = false) String searchTerm,
            @PageableDefault(size = 50, sort = "dataAdicao", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<com.joaopaulo.musicas.dtos.response.UserSongResponse> musicas = userSongService.listarMusicas(userId, searchTerm, pageable);
        return ResponseEntity.ok(musicas);
    }

    @Operation(summary = "Verificar se uma música específica está salva no perfil")
    @GetMapping("/{userId}/songs/{trackId}")
    public ResponseEntity<UserSong> verificarMusica(@PathVariable("userId") Long userId, @PathVariable("trackId") String trackId) {
        return userSongService.verificarMusicaSalva(userId, trackId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}