package com.joaopaulo.musicas.services;

import com.joaopaulo.musicas.dtos.response.HistoricoResponse;
import com.joaopaulo.musicas.entities.HistoricoReproducao;
import com.joaopaulo.musicas.entities.MusicEntity;
import com.joaopaulo.musicas.mappers.MusicMapper;
import com.joaopaulo.musicas.repositories.HistoricoReproducaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class HistoricoReproducaoService {

    private final HistoricoReproducaoRepository historicoRepository;
    private final MusicService musicService;
    private final MusicMapper musicMapper;

    @SuppressWarnings("null")
    public void adicionarAoHistorico(Long userId, String trackId, com.joaopaulo.musicas.dtos.request.MusicSaveRequest request) {
        log.debug("Adicionando track {} ao histórico do usuário {}", trackId, userId);

        // Garante que a música está no nosso catálogo
        try {
            if (request != null && request.getTrackId() != null && request.getNome() != null) {
                musicService.saveCustomMusic(request);
            } else if (trackId != null && trackId.matches("\\d+")) {
                // Tenta salvar da Apple apenas se o ID for numérico (padrão iTunes)
                musicService.saveFromApple(trackId);
            }
        } catch (Exception e) {
            log.warn("Não foi possível pré-salvar a música {} no catálogo: {}", trackId, e.getMessage());
            // Continuamos mesmo sem pré-salvar, para não quebrar a experiência do usuário
        }

        // Se já existir no histórico, remove a antiga para colocar no topo
        removerDoHistorico(userId, trackId);

        @SuppressWarnings("null")
        HistoricoReproducao novoHistorico = HistoricoReproducao.builder()
                .userId(userId)
                .trackId(trackId)
                .dataReproducao(LocalDateTime.now())
                .build();

        historicoRepository.save(java.util.Objects.requireNonNull(novoHistorico));

    }

    public List<HistoricoResponse> obterHistoricoRecente(Long userId) {
        // Pega as 10 reproduções mais recentes
        PageRequest limit = PageRequest.of(0, 10);
        List<HistoricoReproducao> historicos = historicoRepository.findByUserIdOrderByDataReproducaoDesc(userId, limit);

        return historicos.stream().map(historico -> {
            try {
                MusicEntity music = musicService.findById(historico.getTrackId());
                return new HistoricoResponse(
                        historico.getId(),
                        historico.getUserId(),
                        musicMapper.toResponse(music),
                        historico.getDataReproducao()
                );
            } catch (Exception e) {
                log.warn("Música {} não encontrada no catálogo para o histórico: {}", historico.getTrackId(), e.getMessage());
                // Retorna um placeholder para não quebrar a listagem do histórico
                com.joaopaulo.musicas.dtos.response.MusicResponse placeholder = com.joaopaulo.musicas.dtos.response.MusicResponse.builder()
                        .id(historico.getTrackId())
                        .nome("Música indisponível")
                        .artista("Desconhecido")
                        .capaUrl("https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=200&h=200&fit=crop")
                        .build();

                return new HistoricoResponse(
                        historico.getId(),
                        historico.getUserId(),
                        placeholder,
                        historico.getDataReproducao()
                );
            }
        }).toList();
    }

    public void removerDoHistorico(Long userId, String trackId) {
        historicoRepository.findByUserIdAndTrackId(userId, trackId).ifPresent(historicoRepository::delete);
    }
}
