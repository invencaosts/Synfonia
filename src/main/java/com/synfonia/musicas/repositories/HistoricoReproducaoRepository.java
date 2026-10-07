package com.synfonia.musicas.repositories;

import com.synfonia.musicas.entities.HistoricoReproducao;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HistoricoReproducaoRepository extends JpaRepository<HistoricoReproducao, String> {

    List<HistoricoReproducao> findByUserIdOrderByDataReproducaoDesc(Long userId, Pageable pageable);

    Optional<HistoricoReproducao> findByUserIdAndTrackId(Long userId, String trackId);
}
