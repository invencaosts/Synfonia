package com.synfonia.musicas.repositories;

import com.synfonia.musicas.entities.HistoricoReproducao;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HistoricoReproducaoRepository extends MongoRepository<HistoricoReproducao, String> {

    List<HistoricoReproducao> findByUserIdOrderByDataReproducaoDesc(Long userId, Pageable pageable);

    void deleteByUserIdAndTrackId(Long userId, String trackId);
    
    Optional<HistoricoReproducao> findByUserIdAndTrackId(Long userId, String trackId);
}
