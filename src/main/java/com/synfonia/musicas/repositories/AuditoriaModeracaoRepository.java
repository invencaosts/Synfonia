package com.synfonia.musicas.repositories;

import com.synfonia.musicas.entities.AuditoriaModeracao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditoriaModeracaoRepository extends JpaRepository<AuditoriaModeracao, Long> {
    Page<AuditoriaModeracao> findAllByOrderByCriadoEmDesc(Pageable pageable);

    Page<AuditoriaModeracao> findByAlvoUsuarioIdOrderByCriadoEmDesc(Long alvoUsuarioId, Pageable pageable);
}
