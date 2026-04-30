package com.joaopaulo.musicas.controllers;

import com.joaopaulo.musicas.entities.UserSong;
import com.joaopaulo.musicas.enums.MusicSource;
import com.joaopaulo.musicas.services.UserSongService;
import com.joaopaulo.musicas.dtos.response.UserSongResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/me/songs")
@RequiredArgsConstructor
@Tag(name = "Gerenciamento de Músicas Salvas", description = "Endpoints para adicionar e listar músicas salvas no perfil")
public class UserSongController {

    private final UserSongService userSongService;

    @Operation(summary = "Adicionar música ao perfil do usuário logado")
    @PostMapping
    public ResponseEntity<UserSong> adicionarMusica(@RequestBody com.joaopaulo.musicas.dtos.request.MusicSaveRequest request) {
        UserSong userSong = userSongService.adicionarMusica(request);
        return ResponseEntity.ok(userSong);
    }

    @Operation(summary = "Remover música do perfil do usuário logado")
    @DeleteMapping("/{trackId}")
    public ResponseEntity<Void> removerMusica(@PathVariable("trackId") String trackId) {
        userSongService.removerMusica(trackId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Remover músicas do perfil por fonte (Ex: SPOTIFY)")
    @DeleteMapping("/source/{source}")
    public ResponseEntity<Long> removerMusicasPorFonte(@PathVariable("source") MusicSource source) {
        Long count = userSongService.removerMusicasPorFonte(source);
        return ResponseEntity.ok(count);
    }

    @Operation(summary = "Listar músicas salvas pelo usuário logado com busca e paginação")
    @GetMapping
    public ResponseEntity<Page<UserSongResponse>> listarMusicas(
            @RequestParam(value = "q", required = false) String searchTerm,
            @PageableDefault(size = 50, sort = "dataAdicao,desc") Pageable pageable) {
        
        Long userId = userSongService.getLoggedUserId();
        Page<UserSongResponse> musicas = userSongService.listarMusicas(userId, searchTerm, pageable);
        return ResponseEntity.ok(musicas);
    }

    @Operation(summary = "Verificar se uma música específica está salva no perfil")
    @GetMapping("/{trackId}")
    public ResponseEntity<UserSong> verificarMusica(@PathVariable("trackId") String trackId) {
        return userSongService.verificarMusicaSalva(trackId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Obter apenas os IDs das músicas salvas no perfil")
    @GetMapping("/ids")
    public ResponseEntity<java.util.List<String>> obterIdsFavoritos(
            @RequestParam(value = "excludeSource", required = false) MusicSource excludeSource) {
        Long userId = userSongService.getLoggedUserId();
        return ResponseEntity.ok(userSongService.getFavoriteTrackIds(userId, excludeSource));
    }
}
