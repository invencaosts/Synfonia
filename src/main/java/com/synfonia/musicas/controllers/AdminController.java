package com.synfonia.musicas.controllers;

import com.synfonia.musicas.dtos.request.ModeracaoRequest;
import com.synfonia.musicas.dtos.response.AdminUserResponse;
import com.synfonia.musicas.dtos.response.AlbumRatingResponse;
import com.synfonia.musicas.dtos.response.AuditoriaResponse;
import com.synfonia.musicas.dtos.response.RoleResponse;
import com.synfonia.musicas.services.ModerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Painel de moderação. A URL inteira já exige PAINEL_MODERACAO_ACESSAR (SecurityConfig);
 * cada endpoint exige ainda a permissão específica, e o ModerationService confere hierarquia.
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Moderação", description = "Usuários, papéis, conteúdo e auditoria (RBAC)")
public class AdminController {

    private final ModerationService moderationService;

    @Operation(summary = "Busca usuários (inclui privados e suspensos)")
    @PreAuthorize("hasAuthority('USUARIOS_LER')")
    @GetMapping("/usuarios")
    public ResponseEntity<Page<AdminUserResponse>> buscarUsuarios(
            @RequestParam(value = "q", required = false) String termo,
            @RequestParam(value = "banidos", defaultValue = "false") boolean somenteBanidos,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(moderationService.buscarUsuarios(termo, somenteBanidos, page, size));
    }

    @Operation(summary = "Detalhe de um usuário")
    @PreAuthorize("hasAuthority('USUARIOS_LER')")
    @GetMapping("/usuarios/{id}")
    public ResponseEntity<AdminUserResponse> buscarUsuario(@PathVariable Long id) {
        return ResponseEntity.ok(moderationService.buscarUsuario(id));
    }

    @Operation(summary = "Suspende um usuário (dias vazio = permanente)")
    @PreAuthorize("hasAuthority('USUARIOS_BANIR')")
    @PostMapping("/usuarios/{id}/suspensao")
    public ResponseEntity<AdminUserResponse> suspender(@PathVariable Long id, @RequestBody ModeracaoRequest request) {
        return ResponseEntity.ok(moderationService.suspender(id, request));
    }

    @Operation(summary = "Remove a suspensão de um usuário")
    @PreAuthorize("hasAuthority('USUARIOS_BANIR')")
    @PostMapping("/usuarios/{id}/reativacao")
    public ResponseEntity<AdminUserResponse> reativar(@PathVariable Long id, @RequestBody ModeracaoRequest request) {
        return ResponseEntity.ok(moderationService.reativar(id, request));
    }

    @Operation(summary = "Lista os papéis do sistema e quais você pode conceder")
    @GetMapping("/roles")
    public ResponseEntity<List<RoleResponse>> listarRoles() {
        return ResponseEntity.ok(moderationService.listarRoles());
    }

    @Operation(summary = "Concede um papel a um usuário")
    @PreAuthorize("hasAuthority('PAPEIS_GERENCIAR')")
    @PostMapping("/usuarios/{id}/roles/{role}")
    public ResponseEntity<AdminUserResponse> concederRole(@PathVariable Long id, @PathVariable String role,
                                                          @RequestBody ModeracaoRequest request) {
        return ResponseEntity.ok(moderationService.concederRole(id, role, request));
    }

    // POST em vez de DELETE: o motivo vai no corpo (DELETE com corpo é mal suportado por proxies)
    @Operation(summary = "Remove um papel de um usuário")
    @PreAuthorize("hasAuthority('PAPEIS_GERENCIAR')")
    @PostMapping("/usuarios/{id}/roles/{role}/remocao")
    public ResponseEntity<AdminUserResponse> removerRole(@PathVariable Long id, @PathVariable String role,
                                                         @RequestBody ModeracaoRequest request) {
        return ResponseEntity.ok(moderationService.removerRole(id, role, request));
    }

    @Operation(summary = "Oculta uma avaliação da comunidade")
    @PreAuthorize("hasAuthority('CONTEUDO_MODERAR')")
    @PostMapping("/avaliacoes/{id}/ocultar")
    public ResponseEntity<AlbumRatingResponse> ocultarAvaliacao(@PathVariable String id, @RequestBody ModeracaoRequest request) {
        return ResponseEntity.ok(moderationService.ocultarAvaliacao(id, request));
    }

    @Operation(summary = "Reexibe uma avaliação oculta")
    @PreAuthorize("hasAuthority('CONTEUDO_MODERAR')")
    @PostMapping("/avaliacoes/{id}/reexibir")
    public ResponseEntity<AlbumRatingResponse> reexibirAvaliacao(@PathVariable String id, @RequestBody ModeracaoRequest request) {
        return ResponseEntity.ok(moderationService.reexibirAvaliacao(id, request));
    }

    @Operation(summary = "Bloqueia uma playlist na comunidade")
    @PreAuthorize("hasAuthority('CONTEUDO_MODERAR')")
    @PostMapping("/playlists/{id}/bloquear")
    public ResponseEntity<Void> bloquearPlaylist(@PathVariable String id, @RequestBody ModeracaoRequest request) {
        moderationService.bloquearPlaylist(id, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Desbloqueia uma playlist")
    @PreAuthorize("hasAuthority('CONTEUDO_MODERAR')")
    @PostMapping("/playlists/{id}/desbloquear")
    public ResponseEntity<Void> desbloquearPlaylist(@PathVariable String id, @RequestBody ModeracaoRequest request) {
        moderationService.desbloquearPlaylist(id, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Log de auditoria da moderação")
    @PreAuthorize("hasAuthority('AUDITORIA_LER')")
    @GetMapping("/auditoria")
    public ResponseEntity<Page<AuditoriaResponse>> auditoria(
            @RequestParam(value = "usuarioId", required = false) Long usuarioId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "30") int size) {
        return ResponseEntity.ok(moderationService.listarAuditoria(usuarioId, page, size));
    }
}
