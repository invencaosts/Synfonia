package com.synfonia.musicas.repositories;

import com.synfonia.musicas.entities.AlbumRating;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlbumRatingRepository extends JpaRepository<AlbumRating, String> {

    Optional<AlbumRating> findByUserIdAndAlbumKey(Long userId, String albumKey);

    Optional<AlbumRating> findByIdAndUserId(String id, Long userId);

    /** Usado pelo link público de compartilhamento: já traz o autor (precisa do username e das flags de privacidade). */
    @Query("SELECT r FROM AlbumRating r JOIN FETCH r.usuario WHERE r.id = :id")
    Optional<AlbumRating> findComUsuarioById(@Param("id") String id);

    List<AlbumRating> findByUserIdOrderByAtualizadoEmDesc(Long userId);

    Page<AlbumRating> findByUserId(Long userId, Pageable pageable);

    @Query("""
            SELECT r FROM AlbumRating r
            WHERE r.userId = :userId AND (r.oculto IS NULL OR r.oculto = false)
            """)
    Page<AlbumRating> findVisiveisByUserId(@Param("userId") Long userId, Pageable pageable);

    /** [quantidade, média] calculados no banco. Inclui avaliações ocultas. */
    @Query("SELECT COUNT(r), AVG(r.nota) FROM AlbumRating r WHERE r.userId = :userId")
    List<Object[]> resumoNotasByUserId(@Param("userId") Long userId);

    /** [quantidade, média] só das avaliações visíveis na comunidade. */
    @Query("""
            SELECT COUNT(r), AVG(r.nota) FROM AlbumRating r
            WHERE r.userId = :userId AND (r.oculto IS NULL OR r.oculto = false)
            """)
    List<Object[]> resumoNotasVisiveisByUserId(@Param("userId") Long userId);
}
