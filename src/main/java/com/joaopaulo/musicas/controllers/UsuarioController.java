package com.joaopaulo.musicas.controllers;

import com.joaopaulo.musicas.dtos.response.UsuarioResponse;
import com.joaopaulo.musicas.mappers.UsuarioMapper;
import com.joaopaulo.musicas.services.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Usuários", description = "Endpoints para gerenciamento de perfil e dados do usuário")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final UsuarioMapper usuarioMapper;

    @Operation(summary = "Atualiza a música favorita do perfil")
    @PutMapping("/me/favorite-music")
    public ResponseEntity<UsuarioResponse> updateFavoriteMusic(@RequestBody Map<String, Object> favoriteData) {
        var usuario = usuarioService.updateFavoriteMusic(favoriteData);
        return ResponseEntity.ok(usuarioMapper.toResponse(usuario));
    }

    @Operation(summary = "Remove a música favorita do perfil")
    @DeleteMapping("/me/favorite-music")
    public ResponseEntity<Void> removeFavoriteMusic() {
        usuarioService.removeFavoriteMusic();
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Retorna os dados do usuário atual (Validação de Sessão)")
    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> getMe() {
        var usuario = usuarioService.getMe();
        return ResponseEntity.ok(usuarioMapper.toResponse(usuario));
    }

    @Operation(summary = "Atualiza os dados de identidade do perfil")
    @PutMapping("/me")
    public ResponseEntity<UsuarioResponse> updateProfile(@RequestBody Map<String, Object> profileData) {
        var usuario = usuarioService.updateProfile(profileData);
        return ResponseEntity.ok(usuarioMapper.toResponse(usuario));
    }

    @Operation(summary = "Atualiza a foto de perfil")
    @PutMapping("/photo")
    public ResponseEntity<UsuarioResponse> updateProfilePicture(@RequestBody Map<String, String> photoData) {
        var usuario = usuarioService.updateProfilePicture(photoData.get("fotoPerfil"));
        return ResponseEntity.ok(usuarioMapper.toResponse(usuario));
    }

    @Operation(summary = "Desativa a conta do usuário")
    @PostMapping("/me/deactivate")
    public ResponseEntity<Void> deactivateAccount() {
        usuarioService.deactivateAccount();
        return ResponseEntity.noContent().build();
    }
}
