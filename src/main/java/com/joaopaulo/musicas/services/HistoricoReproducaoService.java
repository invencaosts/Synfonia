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

    public void adicionarAoHistorico(Long userId, String trackId, com.joaopaulo.musicas.dtos.request.MusicSaveRequest request) {
        log.info("Adicionando track {} ao histórico do usuário {}", trackId, userId);

        // Garante que a música está no nosso catálogo
        if (request != null) {
            musicService.saveCustomMusic(request);
        } else {
            musicService.saveFromApple(trackId);
        }

        // Se já existir no histórico, remove a antiga para colocar no topo
        removerDoHistorico(userId, trackId);

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
            MusicEntity music = musicService.findById(historico.getTrackId());
            return new HistoricoResponse(
                    historico.getId(),
                    historico.getUserId(),
                    musicMapper.toResponse(music),
                    historico.getDataReproducao()
            );
        }).toList();
    }

    public void removerDoHistorico(Long userId, String trackId) {
        historicoRepository.findByUserIdAndTrackId(userId, trackId).ifPresent(historicoRepository::delete);
    }
}
