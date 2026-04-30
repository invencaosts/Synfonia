package com.joaopaulo.musicas.services;

import com.joaopaulo.musicas.dtos.request.MusicRequest;
import com.joaopaulo.musicas.dtos.wrapper.ItunesSearchWrapper;
import com.joaopaulo.musicas.entities.MusicEntity;
import com.joaopaulo.musicas.exceptions.ExternalServiceException;
import com.joaopaulo.musicas.exceptions.IllegalMusicArgumentsException;
import com.joaopaulo.musicas.exceptions.MusicNotFoundException;
import com.joaopaulo.musicas.mappers.MusicMapper;
import com.joaopaulo.musicas.repositories.MusicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MusicService {

    private final MusicRepository musicRepository;
    private final MusicMapper musicMapper;
    private final RestClient restClient;

    private static final String ALBUM = "album";
    private static final String ARTIST = "artist";
    private static final String TITLE = "title";

    public List<MusicEntity> searchByFilter(String nome, String artista, String album, String tipo, Integer limit) {
        MusicRequest request = new MusicRequest(nome, artista, album, limit);
        validateRequest(request);

        log.info("Iniciando busca por filtro [{}]: {}", tipo, request.getTrackName());

        List<MusicEntity> externalResults;
        try {
            externalResults = fetchFromAppleWithAttribute(request, tipo);
        } catch (Exception e) {
            log.warn("Falha na busca externa (Apple): {}. Retornando apenas resultados locais.", e.getMessage());
            externalResults = Collections.emptyList();
        }

        // Retornamos os resultados externos diretamente, sem priorizar a biblioteca local.
        // A sinalização visual (ícone de coração vs check) continuará funcionando no frontend via ID.
        return sortResultsByRelevance(externalResults, request.getTrackName(), tipo);
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
    public MusicEntity saveCustomMusic(com.joaopaulo.musicas.dtos.request.MusicSaveRequest request) {
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
                            .source(request.getSource() != null ? request.getSource() : com.joaopaulo.musicas.enums.MusicSource.SPOTIFY)
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
                        entity.setSource(com.joaopaulo.musicas.enums.MusicSource.ITUNES);
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