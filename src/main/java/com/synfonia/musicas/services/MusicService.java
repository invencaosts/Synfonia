package com.synfonia.musicas.services;

import com.synfonia.musicas.dtos.request.MusicRequest;
import com.synfonia.musicas.dtos.response.AlbumResponse;
import com.synfonia.musicas.dtos.wrapper.ItunesSearchWrapper;
import com.synfonia.musicas.dtos.wrapper.YtMusicTrackResponse;
import com.synfonia.musicas.entities.MusicEntity;
import com.synfonia.musicas.enums.MusicSource;
import com.synfonia.musicas.exceptions.ExternalServiceException;
import com.synfonia.musicas.exceptions.IllegalMusicArgumentsException;
import com.synfonia.musicas.exceptions.MusicNotFoundException;
import com.synfonia.musicas.mappers.MusicMapper;
import com.synfonia.musicas.repositories.MusicRepository;
import com.synfonia.musicas.util.AlbumKeyUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class MusicService {

    private final MusicRepository musicRepository;
    private final MusicMapper musicMapper;
    private final RestClient restClient;

    @Value("${ytmusic.service.url}")
    private String ytMusicServiceUrl;

    private static final String ALBUM = "album";
    private static final String ARTIST = "artist";
    private static final String TITLE = "title";

    // Hosts de CDN de capa de álbum conhecidos (iTunes/Apple e YouTube Music).
    // Allowlist obrigatória aqui: sem ela, este proxy vira um SSRF genérico.
    private static final Set<String> ALLOWED_IMAGE_HOSTS = Set.of(
            "mzstatic.com", "googleusercontent.com", "ytimg.com", "ggpht.com"
    );

    public ResponseEntity<byte[]> proxyImagem(String url) {
        URI uri;
        try {
            uri = URI.create(url);
        } catch (Exception e) {
            throw new IllegalMusicArgumentsException("URL de imagem inválida.");
        }

        String host = uri.getHost();
        boolean permitido = host != null && ALLOWED_IMAGE_HOSTS.stream().anyMatch(host::endsWith);
        if (!permitido) {
            throw new IllegalMusicArgumentsException("Host de imagem não permitido.");
        }

        try {
            ResponseEntity<byte[]> resposta = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .toEntity(byte[].class);

            MediaType contentType = resposta.getHeaders().getContentType() != null
                    ? resposta.getHeaders().getContentType()
                    : MediaType.IMAGE_JPEG;

            return ResponseEntity.ok()
                    .contentType(contentType)
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                    .body(resposta.getBody());
        } catch (Exception e) {
            log.warn("Falha ao fazer proxy de imagem {}: {}", url, e.getMessage());
            throw new ExternalServiceException("Não foi possível carregar a imagem.");
        }
    }

    public List<MusicEntity> searchByFilter(String nome, String artista, String album, String tipo, Integer limit, MusicSource source) {
        MusicRequest request = new MusicRequest(nome, artista, album, limit);
        validateRequest(request);

        log.info("Iniciando busca por filtro [{}] na fonte [{}]: {}", tipo, source, request.getTrackName());

        List<MusicEntity> externalResults = fetchExternal(request, tipo, source);

        // Retornamos os resultados externos diretamente, sem priorizar a biblioteca local.
        // A sinalização visual (ícone de coração vs check) continuará funcionando no frontend via ID.
        return sortResultsByRelevance(externalResults, request.getTrackName(), tipo);
    }

    private List<MusicEntity> fetchExternal(MusicRequest request, String tipo, MusicSource source) {
        try {
            return source == MusicSource.YOUTUBE_MUSIC
                    ? fetchFromYtMusic(request, tipo)
                    : fetchFromAppleWithAttribute(request, tipo);
        } catch (Exception e) {
            log.warn("Falha na busca externa ({}): {}. Retornando apenas resultados locais.", source, e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<AlbumResponse> searchAlbums(String nome, MusicSource source) {
        MusicRequest request = new MusicRequest(nome, null, null, 50);
        validateRequest(request);

        // Não reutilizamos sortResultsByRelevance aqui: aquele filtro exige a frase de busca
        // inteira como substring contígua de um único campo, o que descarta álbuns legítimos
        // sempre que o título da busca não bate palavra-por-palavra com o catálogo externo
        // (ordem de palavras, acentos, "feat.", etc.). Para álbuns confiamos no próprio
        // ranking de relevância da API externa (iTunes/YT Music).
        List<MusicEntity> faixas = fetchExternal(request, ALBUM, source);

        Map<String, AlbumResponse> agrupadoPorAlbum = new LinkedHashMap<>();
        for (MusicEntity musica : faixas) {
            if (musica.getAlbum() == null || musica.getAlbum().isBlank()) continue;

            String albumKey = AlbumKeyUtil.gerarChave(musica.getSource(), musica.getArtista(), musica.getAlbum());
            agrupadoPorAlbum.putIfAbsent(albumKey, AlbumResponse.builder()
                    .albumKey(albumKey)
                    .artista(musica.getArtista())
                    .albumName(musica.getAlbum())
                    .capaUrl(musica.getCapaUrl())
                    .source(musica.getSource())
                    // No YT Music, a busca com tipo=album já retorna ÁLBUNS (não faixas),
                    // e o "id" desse resultado é o browseId real do álbum — guardamos para
                    // buscar a tracklist completa depois via buscarFaixasDoAlbum. O iTunes
                    // não expõe um id de álbum estável nesse fluxo (tipo=album lá retorna
                    // faixas), então fica null e a tracklist é resolvida por nome+artista.
                    .externalAlbumId(musica.getSource() == MusicSource.YOUTUBE_MUSIC ? musica.getId() : null)
                    .build());
        }
        return new ArrayList<>(agrupadoPorAlbum.values());
    }

    public List<MusicEntity> buscarFaixasDoAlbum(String externalAlbumId, String artista, String albumName, MusicSource source) {
        if (source == MusicSource.YOUTUBE_MUSIC && externalAlbumId != null && !externalAlbumId.isBlank()) {
            return fetchAlbumTracksFromYtMusic(externalAlbumId);
        }
        // iTunes: attribute=albumTerm com entity=song já retorna as faixas corretas do álbum.
        return searchByFilter(albumName, artista, albumName, ALBUM, 100, source);
    }

    private List<MusicEntity> fetchAlbumTracksFromYtMusic(String browseId) {
        try {
            List<YtMusicTrackResponse> results = restClient.get()
                    .uri(ytMusicServiceUrl + "/album/{browseId}", browseId)
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<List<YtMusicTrackResponse>>() {});

            if (results == null) return Collections.emptyList();

            return results.stream()
                    .map(dto -> MusicEntity.builder()
                            .id(dto.getId())
                            .nome(dto.getNome())
                            .artista(dto.getArtista())
                            .album(dto.getAlbum())
                            .capaUrl(dto.getCapaUrl())
                            .previewUrl(dto.getPreviewUrl())
                            .uri(dto.getUri())
                            .source(MusicSource.YOUTUBE_MUSIC)
                            .build())
                    .toList();
        } catch (Exception e) {
            log.error("Erro ao buscar faixas do álbum no YT Music (browseId={}): {}", browseId, e.getMessage(), e);
            throw new ExternalServiceException("Erro ao buscar faixas do álbum no provedor externo (YT Music).");
        }
    }

    private List<MusicEntity> fetchFromYtMusic(MusicRequest request, String tipo) {
        try {
            List<YtMusicTrackResponse> results = restClient.get()
                    .uri(ytMusicServiceUrl + "/search?q={q}&tipo={tipo}&limit={limit}",
                            request.getTrackName(), tipo, request.getLimit())
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<List<YtMusicTrackResponse>>() {});

            if (results == null) return Collections.emptyList();

            return results.stream()
                    .map(dto -> MusicEntity.builder()
                            .id(dto.getId())
                            .nome(dto.getNome())
                            .artista(dto.getArtista())
                            .album(dto.getAlbum())
                            .capaUrl(dto.getCapaUrl())
                            .previewUrl(dto.getPreviewUrl())
                            .uri(dto.getUri())
                            .source(MusicSource.YOUTUBE_MUSIC)
                            .build())
                    .toList();
        } catch (Exception e) {
            log.error("Erro na integração externa com YT Music: {}", e.getMessage(), e);
            throw new ExternalServiceException("Erro ao processar a busca no provedor externo (YT Music).");
        }
    }

    private void validateRequest(MusicRequest request) {
        if (!request.hasAnyParameter()) {
            throw new IllegalMusicArgumentsException("Pelo menos um parâmetro de busca deve ser fornecido.");
        }
    }

    @SuppressWarnings("null")
    public MusicEntity findById(String id) {
        return musicRepository.findById(java.util.Objects.requireNonNull(id))

                .orElseGet(() -> {
                    log.info("Música {} não encontrada no banco. Tentando recuperar da Apple via Lookup.", id);
                    return fetchByTrackIdFromApple(id);
                });
    }

    public List<MusicEntity> findAllByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyList();
        return musicRepository.findAllById(ids);
    }

    // Novo método para quando REALMENTE precisamos tentar um fallback de preview
    @SuppressWarnings("null")
    public MusicEntity findByIdWithPreviewFallback(String id) {
        return musicRepository.findById(java.util.Objects.requireNonNull(id))

                .map(entity -> {
                    if (entity.getPreviewUrl() == null || entity.getPreviewUrl().isEmpty()) {
                        log.info("Solicitado fallback de preview para música {}.", id);
                        try {
                            MusicRequest request = new MusicRequest(entity.getNome(), entity.getArtista(), entity.getAlbum(), 1);
                            List<MusicEntity> appleResults = fetchFromAppleWithAttribute(request, "all");
                            if (!appleResults.isEmpty()) {
                                MusicEntity appleMatch = appleResults.get(0);
                                if (appleMatch.getPreviewUrl() != null) {
                                    entity.setPreviewUrl(appleMatch.getPreviewUrl());
                                    return musicRepository.save(entity);
                                }
                            }
                        } catch (Exception e) {
                            log.warn("Falha no fallback de preview para {}: {}", id, e.getMessage());
                        }
                    }
                    return entity;
                })
                .orElseGet(() -> fetchByTrackIdFromApple(id));
    }

    @SuppressWarnings("null")
    private MusicEntity fetchByTrackIdFromApple(String trackId) {

        try {
            ItunesSearchWrapper wrapper = restClient.get()
                    .uri("https://itunes.apple.com/lookup?id={id}&entity=song", trackId)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36")
                    .header("Accept", "text/javascript, application/json, application/xml, text/xml, */*")
                    .header("Accept-Language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7")
                    .retrieve()
                    .body(ItunesSearchWrapper.class);



            if (wrapper != null && wrapper.getResults() != null && !wrapper.getResults().isEmpty()) {
                MusicEntity entity = musicMapper.toEntity(wrapper.getResults().get(0));
                log.debug("Música {} recuperada com sucesso da Apple. Salvando no catálogo.", trackId);
                return musicRepository.save(entity);
            }
        } catch (Exception e) {
            log.warn("Falha ao tentar recuperar metadados da música {} na Apple: {}", trackId, e.getMessage());
        }
        throw new MusicNotFoundException("Música não encontrada no catálogo local nem no provedor externo.");
    }

    @SuppressWarnings("null")
    public MusicEntity saveFromApple(String trackId) {
        return musicRepository.findById(java.util.Objects.requireNonNull(trackId))

                .orElseGet(() -> fetchByTrackIdFromApple(trackId));
    }

    @SuppressWarnings("null")
    public MusicEntity saveCustomMusic(com.synfonia.musicas.dtos.request.MusicSaveRequest request) {
        return musicRepository.findById(java.util.Objects.requireNonNull(request.getTrackId()))

                .orElseGet(() -> {
                    MusicEntity entity = MusicEntity.builder()
                            .id(request.getTrackId())
                            .nome(request.getNome())
                            .artista(request.getArtista())
                            .album(request.getAlbum())
                            .capaUrl(request.getCapaUrl())
                            .previewUrl(request.getPreviewUrl())
                            .uri(request.getUri())
                            .anoLancamento(request.getAnoLancamento())
                            .source(request.getSource() != null ? request.getSource() : com.synfonia.musicas.enums.MusicSource.SPOTIFY)
                            .build();
                    log.debug("Música personalizada (ex: Spotify) {} salva no catálogo.", request.getTrackId());
                    return musicRepository.save(entity);
                });
    }

    private List<MusicEntity> fetchFromAppleWithAttribute(MusicRequest request, String tipo) {
        String attributeParam = switch (tipo) {
            case TITLE -> "songTerm";
            case ARTIST -> "artistTerm";
            case ALBUM -> "albumTerm";
            default -> null;
        };

        try {
            var uriBuilder = restClient.get()
                    .uri(uri -> {
                        var builder = uri.scheme("https")
                                .host("itunes.apple.com")
                                .path("/search")
                                .queryParam("term", request.getTrackName())
                                .queryParam("limit", request.getLimit())
                                .queryParam("entity", "song");
                        
                        if (attributeParam != null) {
                            builder.queryParam("attribute", attributeParam);
                        }
                        return builder.build();
                    })
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36")
                    .header("Accept", "text/javascript, application/json, application/xml, text/xml, */*")
                    .header("Accept-Language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7");


            ItunesSearchWrapper wrapper = uriBuilder.retrieve().body(ItunesSearchWrapper.class);


            if (wrapper == null || wrapper.getResults() == null || wrapper.getResults().isEmpty()) {
                log.warn("Nenhum resultado encontrado na Apple para: {}", request.getTrackName());
                return Collections.emptyList();
            }

            return wrapper.getResults().stream()
                    .map(dto -> {
                        MusicEntity entity = new MusicEntity(dto, String.valueOf(dto.getTrackId()));
                        entity.setSource(com.synfonia.musicas.enums.MusicSource.ITUNES);
                        return entity;
                    })
                    .toList();

        } catch (Exception e) {
            log.error("Erro na integração externa com Apple: {}", e.getMessage(), e);
            throw new ExternalServiceException("Erro ao processar a busca no provedor externo.");
        }
    }

    private List<MusicEntity> sortResultsByRelevance(List<MusicEntity> results, String searchTerm, String tipo) {
        if (results == null) return Collections.emptyList();
        
        String term = searchTerm.toLowerCase();

        return results.stream()
                .filter(m -> m != null && (
                        (m.getNome() != null && m.getNome().toLowerCase().contains(term)) ||
                        (m.getArtista() != null && m.getArtista().toLowerCase().contains(term)) ||
                        (m.getAlbum() != null && m.getAlbum().toLowerCase().contains(term))
                ))
                .sorted((m1, m2) -> {
                    int score1 = getRelevanceScore(m1, term, tipo);
                    int score2 = getRelevanceScore(m2, term, tipo);
                    if (score1 != score2) return Integer.compare(score1, score2);

                    int m1Penalty = getVersionPenalty(m1);
                    int m2Penalty = getVersionPenalty(m2);
                    if (m1Penalty != m2Penalty) return Integer.compare(m1Penalty, m2Penalty);

                    return 0;
                })
                .toList();
    }

    private int getRelevanceScore(MusicEntity music, String term, String tipo) {
        return switch (tipo) {
            case ARTIST -> getArtistRelevance(music, term);
            case ALBUM -> getAlbumRelevance(music, term);
            case TITLE -> getTitleRelevance(music, term);
            default -> getDefaultRelevance(music, term);
        };
    }

    private int getArtistRelevance(MusicEntity music, String term) {
        if (music.getArtista() != null && music.getArtista().equalsIgnoreCase(term)) return 0;
        if (music.getArtista() != null && music.getArtista().toLowerCase().contains(term)) return 1;
        if (music.getNome() != null && music.getNome().toLowerCase().contains(term)) return 2;
        return 3;
    }

    private int getAlbumRelevance(MusicEntity music, String term) {
        if (music.getAlbum() != null && music.getAlbum().equalsIgnoreCase(term)) return 0;
        if (music.getAlbum() != null && music.getAlbum().toLowerCase().contains(term)) return 1;
        if (music.getNome() != null && music.getNome().toLowerCase().contains(term)) return 2;
        return 3;
    }

    private int getTitleRelevance(MusicEntity music, String term) {
        if (music.getNome() != null && music.getNome().equalsIgnoreCase(term)) return 0;
        if (music.getNome() != null && music.getNome().toLowerCase().contains(term)) return 1;
        return 2;
    }

    private int getDefaultRelevance(MusicEntity music, String term) {
        if (music.getArtista() != null && music.getArtista().equalsIgnoreCase(term)) return 0;
        if (music.getNome() != null && music.getNome().equalsIgnoreCase(term)) return 1;
        if (music.getArtista() != null && music.getArtista().toLowerCase().contains(term)) return 2;
        if (music.getNome() != null && music.getNome().toLowerCase().contains(term)) return 3;
        if (music.getAlbum() != null && music.getAlbum().toLowerCase().contains(term)) return 4;
        return 5;
    }

    private int getVersionPenalty(MusicEntity music) {
        String fullName = (music.getNome() + " " + (music.getAlbum() != null ? music.getAlbum() : "")).toLowerCase();
        
        if (fullName.contains("acapella") || fullName.contains("a cappella")) return 2;
        if (fullName.contains("instrumental")) return 1;
        
        return 0;
    }
}