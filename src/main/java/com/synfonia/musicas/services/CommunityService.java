package com.synfonia.musicas.services;

import com.synfonia.musicas.dtos.response.AlbumRatingResponse;
import com.synfonia.musicas.dtos.response.MusicResponse;
import com.synfonia.musicas.dtos.response.PublicPlaylistResponse;
import com.synfonia.musicas.dtos.response.PublicProfileResponse;
import com.synfonia.musicas.dtos.response.UserSongResponse;
import com.synfonia.musicas.dtos.response.UserSummaryResponse;
import com.synfonia.musicas.entities.AlbumRating;
import com.synfonia.musicas.entities.MusicEntity;
import com.synfonia.musicas.entities.Playlist;
import com.synfonia.musicas.entities.Usuario;
import com.synfonia.musicas.enums.Permissao;
import com.synfonia.musicas.enums.RoleSistema;
import com.synfonia.musicas.exceptions.PlaylistNotFoundException;
import com.synfonia.musicas.exceptions.UsuarioNaoEncontradoException;
import com.synfonia.musicas.mappers.MusicMapper;
import com.synfonia.musicas.mappers.UsuarioMapper;
import com.synfonia.musicas.repositories.AlbumRatingRepository;
import com.synfonia.musicas.repositories.PlaylistRepository;
import com.synfonia.musicas.repositories.UserSongRepository;
import com.synfonia.musicas.repositories.UsuarioRepository;
import com.synfonia.musicas.security.AccessPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Leitura de perfis de outros usuários (comunidade).
 *
 * Regra de visibilidade centralizada aqui: um perfil só aparece para terceiros se a conta
 * estiver ativa, não suspensa e com {@code perfilPublico}. Fora disso responde 404 (e não 403)
 * para não revelar quais usernames existem. O dono sempre enxerga o próprio perfil completo.
 *
 * Moderação: quem tem USUARIOS_BANIR enxerga perfis suspensos; quem tem CONTEUDO_MODERAR
 * enxerga avaliações ocultas e playlists bloqueadas (sinalizadas), para poder revertê-las.
 */
@Service
@RequiredArgsConstructor
public class CommunityService {

    static final int MIN_TERMO_BUSCA = 2;
    static final int MAX_TERMO_BUSCA = 50;
    static final int MAX_PAGE_SIZE = 50;

    private static final String PERFIL_NAO_ENCONTRADO = "Perfil não encontrado";

    private final UsuarioRepository usuarioRepository;
    private final AlbumRatingRepository albumRatingRepository;
    private final PlaylistRepository playlistRepository;
    private final UserSongRepository userSongRepository;
    private final AlbumRatingService albumRatingService;
    private final UserSongService userSongService;
    private final MusicService musicService;
    private final MusicMapper musicMapper;
    private final UsuarioMapper usuarioMapper;
    private final AccessPolicy accessPolicy;

    public Page<UserSummaryResponse> buscarUsuarios(String termo, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), limitarTamanho(size));
        String limpo = termo == null ? "" : termo.trim();
        if (limpo.startsWith("@")) {
            limpo = limpo.substring(1).trim();
        }

        if (limpo.isEmpty()) {
            return usuarioRepository.listarPerfisPublicosRecentes(pageable).map(this::toSummary);
        }
        if (limpo.length() < MIN_TERMO_BUSCA) {
            return Page.empty(pageable);
        }
        if (limpo.length() > MAX_TERMO_BUSCA) {
            limpo = limpo.substring(0, MAX_TERMO_BUSCA);
        }
        return usuarioRepository.buscarPerfisPublicos(escaparLike(limpo), pageable).map(this::toSummary);
    }

    public PublicProfileResponse buscarPerfil(String username, Long viewerId) {
        Usuario usuario = resolverPerfilVisivel(username, viewerId);
        boolean dono = isDono(usuario, viewerId);
        boolean avaliacoesVisiveis = dono || !Boolean.FALSE.equals(usuario.getShowAvaliacoes());
        boolean curtidasVisiveis = dono || !Boolean.FALSE.equals(usuario.getShowCurtidas());
        boolean mostrarNomePessoal = dono || !Boolean.FALSE.equals(usuario.getShowPersonalName());
        boolean verOcultos = dono || accessPolicy.tem(Permissao.CONTEUDO_MODERAR);
        boolean verSuspensao = accessPolicy.tem(Permissao.USUARIOS_BANIR);
        boolean podeModerar = accessPolicy.atualOpcional()
                .map(ator -> AccessPolicy.podeAgirSobre(ator, usuario)
                        && (AccessPolicy.tem(ator, Permissao.CONTEUDO_MODERAR) || AccessPolicy.tem(ator, Permissao.USUARIOS_BANIR)))
                .orElse(false);

        PublicProfileResponse.PublicProfileResponseBuilder builder = PublicProfileResponse.builder()
                .id(usuario.getId())
                .username(usuario.getUsername())
                .displayName(usuario.getDisplayName())
                .personalName(mostrarNomePessoal ? usuario.getPersonalName() : null)
                .fotoPerfil(usuario.getFotoPerfil())
                .dataCriacao(usuario.getDataCriacao())
                .socialLinks(usuarioMapper.buildSocialLinks(usuario))
                .favoriteTrackId(usuario.getFavoriteTrackId())
                .favoriteTrackName(usuario.getFavoriteTrackName())
                .favoriteTrackArtist(usuario.getFavoriteTrackArtist())
                .favoriteTrackCapaUrl(usuario.getFavoriteTrackCapaUrl())
                .favoriteTrackPreviewUrl(usuario.getFavoriteTrackPreviewUrl())
                .proprioPerfil(dono)
                .avaliacoesVisiveis(avaliacoesVisiveis)
                .curtidasVisiveis(curtidasVisiveis)
                .totalPlaylistsPublicas(playlistRepository.countPublicasVisiveisByUserId(usuario.getId()))
                .papeis(usuario.getRolesEfetivas().stream()
                        .filter(r -> r != RoleSistema.USER)
                        .map(Enum::name)
                        .toList())
                .podeModerar(podeModerar);

        if (verSuspensao) {
            builder.banido(usuario.isBanidoAgora())
                    .banidoAte(usuario.getBanidoAte())
                    .banidoMotivo(usuario.getBanidoMotivo());
        }

        if (avaliacoesVisiveis) {
            // Contagem e média calculadas no banco: [COUNT, AVG]
            List<Object[]> resumo = verOcultos
                    ? albumRatingRepository.resumoNotasByUserId(usuario.getId())
                    : albumRatingRepository.resumoNotasVisiveisByUserId(usuario.getId());
            Object[] linha = resumo.isEmpty() ? new Object[]{0L, null} : resumo.get(0);
            Number media = (Number) linha[1];
            builder.totalAvaliacoes(((Number) linha[0]).longValue())
                    .mediaAvaliacoes(media == null ? null : Math.round(media.doubleValue() * 10) / 10.0);
        }
        if (curtidasVisiveis) {
            builder.totalCurtidas(userSongRepository.countByUserId(usuario.getId()));
        }
        return builder.build();
    }

    public Page<AlbumRatingResponse> listarAvaliacoes(String username, Long viewerId, String ordem, int page, int size) {
        Usuario usuario = resolverPerfilVisivel(username, viewerId);
        if (!isDono(usuario, viewerId) && Boolean.FALSE.equals(usuario.getShowAvaliacoes())) {
            return Page.empty(PageRequest.of(0, limitarTamanho(size)));
        }

        Sort sort = "nota".equalsIgnoreCase(ordem)
                ? Sort.by(Sort.Order.desc("nota"), Sort.Order.desc("atualizadoEm"))
                : Sort.by(Sort.Order.desc("atualizadoEm"));
        Pageable pageable = PageRequest.of(Math.max(page, 0), limitarTamanho(size), sort);

        boolean verOcultos = isDono(usuario, viewerId) || accessPolicy.tem(Permissao.CONTEUDO_MODERAR);
        Page<AlbumRating> pagina = verOcultos
                ? albumRatingRepository.findByUserId(usuario.getId(), pageable)
                : albumRatingRepository.findVisiveisByUserId(usuario.getId(), pageable);
        return pagina.map(albumRatingService::toResponse);
    }

    public Page<UserSongResponse> listarCurtidas(String username, Long viewerId, int page, int size) {
        Usuario usuario = resolverPerfilVisivel(username, viewerId);
        if (!isDono(usuario, viewerId) && Boolean.FALSE.equals(usuario.getShowCurtidas())) {
            return Page.empty(PageRequest.of(0, limitarTamanho(size)));
        }

        Pageable pageable = PageRequest.of(Math.max(page, 0), limitarTamanho(size), Sort.by(Sort.Order.desc("dataAdicao")));
        return userSongService.listarMusicas(usuario.getId(), pageable);
    }

    public List<PublicPlaylistResponse> listarPlaylists(String username, Long viewerId) {
        Usuario usuario = resolverPerfilVisivel(username, viewerId);
        return playlistsPublicas(usuario, viewerId);
    }

    public PublicPlaylistResponse buscarPlaylist(String username, String playlistId, Long viewerId) {
        Usuario usuario = resolverPerfilVisivel(username, viewerId);
        Playlist playlist = playlistRepository.findByIdAndUserIdAndPublicoTrue(playlistId, usuario.getId())
                .filter(p -> !Boolean.TRUE.equals(p.getBloqueadaModeracao()) || verBloqueadas(usuario, viewerId))
                .orElseThrow(() -> new PlaylistNotFoundException("Playlist não encontrada"));

        List<String> trackIds = playlist.getTrackIds() == null ? List.of() : playlist.getTrackIds();
        Map<String, MusicEntity> porId = musicService.findAllByIds(trackIds).stream()
                .collect(Collectors.toMap(MusicEntity::getId, Function.identity(), (a, b) -> a));

        // Mantém a ordem da playlist e descarta faixas que sumiram do catálogo
        List<MusicResponse> tracks = trackIds.stream()
                .map(porId::get)
                .filter(Objects::nonNull)
                .map(musicMapper::toResponse)
                .toList();

        return toPublicPlaylist(playlist, tracks);
    }

    /**
     * Usado pelo endpoint legado {@code GET /playlists/public/{userId}}.
     */
    public List<PublicPlaylistResponse> listarPlaylistsPorId(Long userId, Long viewerId) {
        Usuario usuario = usuarioRepository.findById(Objects.requireNonNull(userId))
                .orElseThrow(() -> new UsuarioNaoEncontradoException(PERFIL_NAO_ENCONTRADO));
        garantirVisivel(usuario, viewerId);
        return playlistsPublicas(usuario, viewerId);
    }

    private List<PublicPlaylistResponse> playlistsPublicas(Usuario usuario, Long viewerId) {
        List<Playlist> playlists = verBloqueadas(usuario, viewerId)
                ? playlistRepository.findByUserIdAndPublicoTrue(usuario.getId())
                : playlistRepository.findPublicasVisiveisByUserId(usuario.getId());
        return playlists.stream().map(playlist -> toPublicPlaylist(playlist, null)).toList();
    }

    private boolean verBloqueadas(Usuario usuario, Long viewerId) {
        return isDono(usuario, viewerId) || accessPolicy.tem(Permissao.CONTEUDO_MODERAR);
    }

    private Usuario resolverPerfilVisivel(String username, Long viewerId) {
        if (username == null || username.isBlank()) {
            throw new UsuarioNaoEncontradoException(PERFIL_NAO_ENCONTRADO);
        }
        Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(username.trim())
                .orElseThrow(() -> new UsuarioNaoEncontradoException(PERFIL_NAO_ENCONTRADO));
        garantirVisivel(usuario, viewerId);
        return usuario;
    }

    private void garantirVisivel(Usuario usuario, Long viewerId) {
        if (isDono(usuario, viewerId)) {
            return;
        }
        if (!usuario.isAtivo() || Boolean.FALSE.equals(usuario.getPerfilPublico())) {
            throw new UsuarioNaoEncontradoException(PERFIL_NAO_ENCONTRADO);
        }
        if (usuario.isBanidoAgora() && !accessPolicy.tem(Permissao.USUARIOS_BANIR)) {
            throw new UsuarioNaoEncontradoException(PERFIL_NAO_ENCONTRADO);
        }
    }

    private boolean isDono(Usuario usuario, Long viewerId) {
        return viewerId != null && viewerId.equals(usuario.getId());
    }

    private int limitarTamanho(int size) {
        if (size <= 0) return 20;
        return Math.min(size, MAX_PAGE_SIZE);
    }

    static String escaparLike(String termo) {
        return termo.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private UserSummaryResponse toSummary(Usuario usuario) {
        return UserSummaryResponse.builder()
                .id(usuario.getId())
                .username(usuario.getUsername())
                .displayName(usuario.getDisplayName())
                .fotoPerfil(usuario.getFotoPerfil())
                .build();
    }

    private PublicPlaylistResponse toPublicPlaylist(Playlist playlist, List<MusicResponse> tracks) {
        List<String> trackIds = playlist.getTrackIds() == null ? List.of() : playlist.getTrackIds();
        return PublicPlaylistResponse.builder()
                .id(playlist.getId())
                .userId(playlist.getUserId())
                .nome(playlist.getNome())
                .vibe(playlist.getVibe())
                .capaUrl(playlist.getCapaUrl())
                .trackIds(trackIds)
                .totalMusicas(trackIds.size())
                .tracks(tracks)
                .bloqueada(Boolean.TRUE.equals(playlist.getBloqueadaModeracao()))
                .bloqueioMotivo(playlist.getBloqueioMotivo())
                .build();
    }
}
