package com.synfonia.musicas.controllers;

import com.synfonia.musicas.dtos.response.AlbumRatingResponse;
import com.synfonia.musicas.dtos.response.PublicPlaylistResponse;
import com.synfonia.musicas.dtos.response.PublicProfileResponse;
import com.synfonia.musicas.dtos.response.UserSongResponse;
import com.synfonia.musicas.dtos.response.UserSummaryResponse;
import com.synfonia.musicas.security.UsuarioDetails;
import com.synfonia.musicas.services.CommunityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/community")
@RequiredArgsConstructor
@Tag(name = "Comunidade", description = "Busca de usuários e leitura de perfis públicos")
public class CommunityController {

    private final CommunityService communityService;

    @Operation(summary = "Busca usuários por username ou nome de exibição (vazio = perfis recentes)")
    @GetMapping("/users")
    public ResponseEntity<Page<UserSummaryResponse>> buscarUsuarios(
            @Parameter(description = "Termo de busca, mínimo 2 caracteres") @RequestParam(value = "q", required = false) String termo,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        return ResponseEntity.ok(communityService.buscarUsuarios(termo, page, size));
    }

    @Operation(summary = "Perfil público de um usuário")
    @GetMapping("/users/{username}")
    public ResponseEntity<PublicProfileResponse> buscarPerfil(
            @AuthenticationPrincipal UsuarioDetails userDetails,
            @PathVariable String username) {

        return ResponseEntity.ok(communityService.buscarPerfil(username, userDetails.getId()));
    }

    @Operation(summary = "Avaliações de álbum de um usuário")
    @GetMapping("/users/{username}/avaliacoes")
    public ResponseEntity<Page<AlbumRatingResponse>> listarAvaliacoes(
            @AuthenticationPrincipal UsuarioDetails userDetails,
            @PathVariable String username,
            @Parameter(description = "recentes (padrão) ou nota") @RequestParam(value = "ordem", defaultValue = "recentes") String ordem,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        return ResponseEntity.ok(communityService.listarAvaliacoes(username, userDetails.getId(), ordem, page, size));
    }

    @Operation(summary = "Músicas curtidas de um usuário")
    @GetMapping("/users/{username}/curtidas")
    public ResponseEntity<Page<UserSongResponse>> listarCurtidas(
            @AuthenticationPrincipal UsuarioDetails userDetails,
            @PathVariable String username,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "30") int size) {

        return ResponseEntity.ok(communityService.listarCurtidas(username, userDetails.getId(), page, size));
    }

    @Operation(summary = "Playlists públicas de um usuário")
    @GetMapping("/users/{username}/playlists")
    public ResponseEntity<List<PublicPlaylistResponse>> listarPlaylists(
            @AuthenticationPrincipal UsuarioDetails userDetails,
            @PathVariable String username) {

        return ResponseEntity.ok(communityService.listarPlaylists(username, userDetails.getId()));
    }

    @Operation(summary = "Detalhe de uma playlist pública com as faixas resolvidas")
    @GetMapping("/users/{username}/playlists/{playlistId}")
    public ResponseEntity<PublicPlaylistResponse> buscarPlaylist(
            @AuthenticationPrincipal UsuarioDetails userDetails,
            @PathVariable String username,
            @PathVariable String playlistId) {

        return ResponseEntity.ok(communityService.buscarPlaylist(username, playlistId, userDetails.getId()));
    }
}
