package com.synfonia.musicas.services;

import com.synfonia.musicas.dtos.request.ModeracaoRequest;
import com.synfonia.musicas.dtos.response.AdminUserResponse;
import com.synfonia.musicas.dtos.response.AlbumRatingResponse;
import com.synfonia.musicas.dtos.response.AuditoriaResponse;
import com.synfonia.musicas.dtos.response.RoleResponse;
import com.synfonia.musicas.entities.AlbumRating;
import com.synfonia.musicas.entities.AuditoriaModeracao;
import com.synfonia.musicas.entities.Playlist;
import com.synfonia.musicas.entities.Usuario;
import com.synfonia.musicas.enums.Permissao;
import com.synfonia.musicas.enums.RoleSistema;
import com.synfonia.musicas.exceptions.AlbumRatingNotFoundException;
import com.synfonia.musicas.exceptions.PlaylistNotFoundException;
import com.synfonia.musicas.exceptions.UsuarioNaoEncontradoException;
import com.synfonia.musicas.repositories.AlbumRatingRepository;
import com.synfonia.musicas.repositories.AuditoriaModeracaoRepository;
import com.synfonia.musicas.repositories.PlaylistRepository;
import com.synfonia.musicas.repositories.UsuarioRepository;
import com.synfonia.musicas.security.AccessPolicy;
import com.synfonia.musicas.security.UsuarioDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Ações de moderação e administração. Cada método:
 * 1. confere a permissão (defesa em profundidade além do @PreAuthorize do controller);
 * 2. confere a hierarquia via {@link AccessPolicy};
 * 3. exige motivo;
 * 4. grava auditoria na mesma transação.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModerationService {

    static final int MOTIVO_MIN = 5;
    static final int MOTIVO_MAX = 500;
    static final int DIAS_MAX = 3650;
    static final int MAX_PAGE_SIZE = 50;

    private final UsuarioRepository usuarioRepository;
    private final AlbumRatingRepository albumRatingRepository;
    private final PlaylistRepository playlistRepository;
    private final AuditoriaModeracaoRepository auditoriaRepository;
    private final AlbumRatingService albumRatingService;
    private final AccessPolicy accessPolicy;

    // ===== Usuários =====

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> buscarUsuarios(String termo, boolean somenteBanidos, int page, int size) {
        UsuarioDetails ator = accessPolicy.atual();
        accessPolicy.exigir(ator, Permissao.USUARIOS_LER);
        boolean dadosSensiveis = AccessPolicy.tem(ator, Permissao.USUARIOS_DADOS_SENSIVEIS);

        String limpo = termo == null ? "" : termo.trim().replaceFirst("^@", "");
        if (limpo.length() > 100) limpo = limpo.substring(0, 100);
        Pageable pageable = PageRequest.of(Math.max(page, 0), limitar(size));

        return usuarioRepository
                .buscarParaModeracao(CommunityService.escaparLike(limpo), dadosSensiveis, somenteBanidos, pageable)
                .map(u -> toAdminUser(u, ator, dadosSensiveis));
    }

    @Transactional(readOnly = true)
    public AdminUserResponse buscarUsuario(Long id) {
        UsuarioDetails ator = accessPolicy.atual();
        accessPolicy.exigir(ator, Permissao.USUARIOS_LER);
        return toAdminUser(carregarUsuario(id), ator, AccessPolicy.tem(ator, Permissao.USUARIOS_DADOS_SENSIVEIS));
    }

    @Transactional
    public AdminUserResponse suspender(Long id, ModeracaoRequest request) {
        UsuarioDetails ator = accessPolicy.atual();
        accessPolicy.exigir(ator, Permissao.USUARIOS_BANIR);
        Usuario alvo = carregarUsuario(id);
        accessPolicy.exigirSuperioridade(ator, alvo);
        String motivo = validarMotivo(request);

        Integer dias = request.getDias();
        if (dias != null && (dias < 1 || dias > DIAS_MAX)) {
            throw new IllegalArgumentException("Duração da suspensão deve ser entre 1 e " + DIAS_MAX + " dias (ou vazia para permanente).");
        }

        alvo.setBanido(true);
        alvo.setBanidoAte(dias == null ? null : LocalDateTime.now().plusDays(dias));
        alvo.setBanidoMotivo(motivo);
        usuarioRepository.save(alvo);

        auditar(ator, "USUARIO_SUSPENDER", alvo.getId(), "USUARIO", String.valueOf(alvo.getId()), motivo,
                dias == null ? "Permanente" : dias + " dia(s)");
        return toAdminUser(alvo, ator, AccessPolicy.tem(ator, Permissao.USUARIOS_DADOS_SENSIVEIS));
    }

    @Transactional
    public AdminUserResponse reativar(Long id, ModeracaoRequest request) {
        UsuarioDetails ator = accessPolicy.atual();
        accessPolicy.exigir(ator, Permissao.USUARIOS_BANIR);
        Usuario alvo = carregarUsuario(id);
        accessPolicy.exigirSuperioridade(ator, alvo);
        String motivo = validarMotivo(request);

        if (!Boolean.TRUE.equals(alvo.getBanido())) {
            throw new IllegalStateException("Este usuário não está suspenso.");
        }
        alvo.setBanido(false);
        alvo.setBanidoAte(null);
        alvo.setBanidoMotivo(null);
        usuarioRepository.save(alvo);

        auditar(ator, "USUARIO_REATIVAR", alvo.getId(), "USUARIO", String.valueOf(alvo.getId()), motivo, null);
        return toAdminUser(alvo, ator, AccessPolicy.tem(ator, Permissao.USUARIOS_DADOS_SENSIVEIS));
    }

    // ===== Papéis =====

    public List<RoleResponse> listarRoles() {
        UsuarioDetails ator = accessPolicy.atual();
        accessPolicy.exigir(ator, Permissao.PAINEL_MODERACAO_ACESSAR);
        boolean gerencia = AccessPolicy.tem(ator, Permissao.PAPEIS_GERENCIAR);
        return Arrays.stream(RoleSistema.values())
                .map(r -> new RoleResponse(
                        r.name(), r.getDescricao(), r.getNivel(),
                        r.getPermissoes().stream().map(Enum::name).sorted().toList(),
                        gerencia && AccessPolicy.podeGerenciarRole(ator, r)))
                .toList();
    }

    @Transactional
    public AdminUserResponse concederRole(Long id, String nomeRole, ModeracaoRequest request) {
        return alterarRole(id, nomeRole, request, true);
    }

    @Transactional
    public AdminUserResponse removerRole(Long id, String nomeRole, ModeracaoRequest request) {
        return alterarRole(id, nomeRole, request, false);
    }

    private AdminUserResponse alterarRole(Long id, String nomeRole, ModeracaoRequest request, boolean conceder) {
        UsuarioDetails ator = accessPolicy.atual();
        accessPolicy.exigir(ator, Permissao.PAPEIS_GERENCIAR);
        RoleSistema role = parseRole(nomeRole);
        accessPolicy.exigirPodeGerenciarRole(ator, role);
        Usuario alvo = carregarUsuario(id);
        accessPolicy.exigirSuperioridade(ator, alvo);
        String motivo = validarMotivo(request);

        boolean mudou = conceder ? alvo.getRoles().add(role) : alvo.getRoles().remove(role);
        if (!mudou) {
            throw new IllegalStateException(conceder
                    ? "O usuário já tem o papel " + role.name() + "."
                    : "O usuário não tem o papel " + role.name() + ".");
        }
        usuarioRepository.save(alvo);

        auditar(ator, conceder ? "PAPEL_CONCEDER" : "PAPEL_REMOVER", alvo.getId(), "USUARIO",
                String.valueOf(alvo.getId()), motivo, role.name());
        return toAdminUser(alvo, ator, AccessPolicy.tem(ator, Permissao.USUARIOS_DADOS_SENSIVEIS));
    }

    // ===== Conteúdo =====

    public AlbumRatingResponse ocultarAvaliacao(String ratingId, ModeracaoRequest request) {
        return alterarVisibilidadeAvaliacao(ratingId, request, true);
    }

    public AlbumRatingResponse reexibirAvaliacao(String ratingId, ModeracaoRequest request) {
        return alterarVisibilidadeAvaliacao(ratingId, request, false);
    }

    private AlbumRatingResponse alterarVisibilidadeAvaliacao(String ratingId, ModeracaoRequest request, boolean ocultar) {
        UsuarioDetails ator = accessPolicy.atual();
        accessPolicy.exigir(ator, Permissao.CONTEUDO_MODERAR);
        AlbumRating rating = albumRatingRepository.findById(Objects.requireNonNull(ratingId))
                .orElseThrow(() -> new AlbumRatingNotFoundException("Avaliação não encontrada."));
        accessPolicy.exigirSuperioridade(ator, carregarUsuario(rating.getUserId()));
        String motivo = validarMotivo(request);

        if (ocultar == Boolean.TRUE.equals(rating.getOculto())) {
            throw new IllegalStateException(ocultar ? "A avaliação já está oculta." : "A avaliação não está oculta.");
        }
        rating.setOculto(ocultar ? Boolean.TRUE : null);
        rating.setOcultoMotivo(ocultar ? motivo : null);
        rating.setOcultoPor(ocultar ? ator.getId() : null);
        rating.setOcultoEm(ocultar ? LocalDateTime.now() : null);
        AlbumRating salvo = albumRatingRepository.save(rating);

        auditar(ator, ocultar ? "AVALIACAO_OCULTAR" : "AVALIACAO_REEXIBIR", rating.getUserId(), "AVALIACAO",
                rating.getId(), motivo, rating.getArtista() + " — " + rating.getAlbumName());
        return albumRatingService.toResponse(salvo);
    }

    public void bloquearPlaylist(String playlistId, ModeracaoRequest request) {
        alterarBloqueioPlaylist(playlistId, request, true);
    }

    public void desbloquearPlaylist(String playlistId, ModeracaoRequest request) {
        alterarBloqueioPlaylist(playlistId, request, false);
    }

    private void alterarBloqueioPlaylist(String playlistId, ModeracaoRequest request, boolean bloquear) {
        UsuarioDetails ator = accessPolicy.atual();
        accessPolicy.exigir(ator, Permissao.CONTEUDO_MODERAR);
        Playlist playlist = playlistRepository.findById(Objects.requireNonNull(playlistId))
                .orElseThrow(() -> new PlaylistNotFoundException("Playlist não encontrada"));
        accessPolicy.exigirSuperioridade(ator, carregarUsuario(playlist.getUserId()));
        String motivo = validarMotivo(request);

        if (bloquear == Boolean.TRUE.equals(playlist.getBloqueadaModeracao())) {
            throw new IllegalStateException(bloquear ? "A playlist já está bloqueada." : "A playlist não está bloqueada.");
        }
        playlist.setBloqueadaModeracao(bloquear ? Boolean.TRUE : null);
        playlist.setBloqueioMotivo(bloquear ? motivo : null);
        playlistRepository.save(playlist);

        auditar(ator, bloquear ? "PLAYLIST_BLOQUEAR" : "PLAYLIST_DESBLOQUEAR", playlist.getUserId(), "PLAYLIST",
                playlist.getId(), motivo, playlist.getNome());
    }

    // ===== Auditoria =====

    @Transactional(readOnly = true)
    public Page<AuditoriaResponse> listarAuditoria(Long alvoUsuarioId, int page, int size) {
        UsuarioDetails ator = accessPolicy.atual();
        accessPolicy.exigir(ator, Permissao.AUDITORIA_LER);
        Pageable pageable = PageRequest.of(Math.max(page, 0), limitar(size));

        Page<AuditoriaModeracao> registros = alvoUsuarioId == null
                ? auditoriaRepository.findAllByOrderByCriadoEmDesc(pageable)
                : auditoriaRepository.findByAlvoUsuarioIdOrderByCriadoEmDesc(alvoUsuarioId, pageable);

        Set<Long> ids = registros.stream()
                .flatMap(a -> java.util.stream.Stream.of(a.getAtorId(), a.getAlvoUsuarioId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> usernames = usuarioRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Usuario::getId, Usuario::getUsername));

        return registros.map(a -> AuditoriaResponse.builder()
                .id(a.getId())
                .atorId(a.getAtorId())
                .atorUsername(a.getAtorId() == null ? "sistema" : usernames.get(a.getAtorId()))
                .acao(a.getAcao())
                .alvoUsuarioId(a.getAlvoUsuarioId())
                .alvoUsername(usernames.get(a.getAlvoUsuarioId()))
                .alvoTipo(a.getAlvoTipo())
                .alvoId(a.getAlvoId())
                .motivo(a.getMotivo())
                .detalhe(a.getDetalhe())
                .criadoEm(a.getCriadoEm())
                .build());
    }

    // ===== Auxiliares =====

    private Usuario carregarUsuario(Long id) {
        return usuarioRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário não encontrado"));
    }

    private void auditar(UsuarioDetails ator, String acao, Long alvoUsuarioId, String alvoTipo, String alvoId,
                         String motivo, String detalhe) {
        auditoriaRepository.save(AuditoriaModeracao.builder()
                .atorId(ator.getId())
                .acao(acao)
                .alvoUsuarioId(alvoUsuarioId)
                .alvoTipo(alvoTipo)
                .alvoId(alvoId)
                .motivo(motivo)
                .detalhe(detalhe)
                .build());
        log.info("[Moderacao] ator={} acao={} alvoUsuario={} alvo={}:{}", ator.getId(), acao, alvoUsuarioId, alvoTipo, alvoId);
    }

    static String validarMotivo(ModeracaoRequest request) {
        String motivo = request == null || request.getMotivo() == null ? "" : request.getMotivo().trim();
        if (motivo.length() < MOTIVO_MIN || motivo.length() > MOTIVO_MAX) {
            throw new IllegalArgumentException("Informe um motivo entre " + MOTIVO_MIN + " e " + MOTIVO_MAX + " caracteres.");
        }
        return motivo;
    }

    private static RoleSistema parseRole(String nome) {
        try {
            return RoleSistema.valueOf(nome == null ? "" : nome.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Papel inexistente: " + nome);
        }
    }

    private static int limitar(int size) {
        if (size <= 0) return 20;
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private AdminUserResponse toAdminUser(Usuario u, UsuarioDetails ator, boolean dadosSensiveis) {
        boolean superior = AccessPolicy.podeAgirSobre(ator, u);
        boolean gerencia = superior && AccessPolicy.tem(ator, Permissao.PAPEIS_GERENCIAR);
        Map<RoleSistema, Boolean> tem = Arrays.stream(RoleSistema.values())
                .collect(Collectors.toMap(Function.identity(), r -> u.getRoles().contains(r)));

        return AdminUserResponse.builder()
                .id(u.getId())
                .username(u.getUsername())
                .displayName(u.getDisplayName())
                .fotoPerfil(u.getFotoPerfil())
                .roles(u.getNomesRoles().stream().toList())
                .nivel(u.getNivelMaximo())
                .ativo(u.isAtivo())
                .perfilPublico(!Boolean.FALSE.equals(u.getPerfilPublico()))
                .banido(u.isBanidoAgora())
                .banidoAte(u.getBanidoAte())
                .banidoMotivo(u.getBanidoMotivo())
                .dataCriacao(u.getDataCriacao())
                .email(dadosSensiveis ? u.getEmail() : null)
                .ultimoLogin(dadosSensiveis ? u.getUltimoLogin() : null)
                .podeSuspender(superior && AccessPolicy.tem(ator, Permissao.USUARIOS_BANIR))
                .rolesConcediveis(gerencia ? Arrays.stream(RoleSistema.values())
                        .filter(r -> AccessPolicy.podeGerenciarRole(ator, r) && !tem.get(r))
                        .map(Enum::name).toList() : List.of())
                .rolesRemoviveis(gerencia ? Arrays.stream(RoleSistema.values())
                        .filter(r -> AccessPolicy.podeGerenciarRole(ator, r) && tem.get(r))
                        .map(Enum::name).toList() : List.of())
                .build();
    }
}
