package com.synfonia.musicas.services;

import com.synfonia.musicas.dtos.request.AlbumRatingRequest;
import com.synfonia.musicas.dtos.response.AlbumRatingResponse;
import com.synfonia.musicas.entities.AlbumRating;
import com.synfonia.musicas.exceptions.AlbumRatingNotFoundException;
import com.synfonia.musicas.repositories.AlbumRatingRepository;
import com.synfonia.musicas.util.AlbumKeyUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlbumRatingService {

    private final AlbumRatingRepository albumRatingRepository;

    public AlbumRatingResponse salvar(Long userId, AlbumRatingRequest request) {
        String albumKey = AlbumKeyUtil.gerarChave(request.getSource(), request.getArtista(), request.getAlbumName());
        LocalDateTime agora = LocalDateTime.now();

        AlbumRating rating = albumRatingRepository.findByUserIdAndAlbumKey(userId, albumKey)
                .orElseGet(() -> AlbumRating.builder()
                        .userId(userId)
                        .albumKey(albumKey)
                        .criadoEm(agora)
                        .build());

        rating.setArtista(request.getArtista());
        rating.setAlbumName(request.getAlbumName());
        rating.setCapaUrl(request.getCapaUrl());
        rating.setSource(request.getSource());
        rating.setNota(request.getNota());
        rating.setTitulo(request.getTitulo());
        rating.setReview(request.getReview());
        rating.setAtualizadoEm(agora);

        AlbumRating salvo = albumRatingRepository.save(rating);
        log.info("Avaliação de álbum salva: usuário={}, albumKey={}, nota={}", userId, albumKey, request.getNota());
        return toResponse(salvo);
    }

    public AlbumRatingResponse buscar(Long userId, String albumKey) {
        return albumRatingRepository.findByUserIdAndAlbumKey(userId, albumKey)
                .map(this::toResponse)
                .orElseThrow(() -> new AlbumRatingNotFoundException("Nenhuma avaliação encontrada para este álbum."));
    }

    public List<AlbumRatingResponse> listar(Long userId) {
        return albumRatingRepository.findByUserIdOrderByAtualizadoEmDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    public void excluir(Long userId, String albumKey) {
        AlbumRating rating = albumRatingRepository.findByUserIdAndAlbumKey(userId, albumKey)
                .orElseThrow(() -> new AlbumRatingNotFoundException("Nenhuma avaliação encontrada para este álbum."));
        albumRatingRepository.delete(rating);
        log.info("Avaliação de álbum excluída: usuário={}, albumKey={}", userId, albumKey);
    }

    private AlbumRatingResponse toResponse(AlbumRating rating) {
        return AlbumRatingResponse.builder()
                .id(rating.getId())
                .albumKey(rating.getAlbumKey())
                .artista(rating.getArtista())
                .albumName(rating.getAlbumName())
                .capaUrl(rating.getCapaUrl())
                .source(rating.getSource())
                .nota(rating.getNota())
                .titulo(rating.getTitulo())
                .review(rating.getReview())
                .criadoEm(rating.getCriadoEm())
                .atualizadoEm(rating.getAtualizadoEm())
                .build();
    }
}
