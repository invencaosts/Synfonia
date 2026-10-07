package com.synfonia.musicas.repositories;

import com.synfonia.musicas.entities.Playlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlaylistRepository extends JpaRepository<Playlist, String> {
    // Busca todas as playlists de um usuário
    List<Playlist> findByUserId(Long userId);

    // Busca apenas as playlists públicas de um usuário (para o perfil)
    List<Playlist> findByUserIdAndPublicoTrue(Long userId);

    Optional<Playlist> findByIdAndUserIdAndPublicoTrue(String id, Long userId);

    @Query("""
            SELECT p FROM Playlist p
            WHERE p.userId = :userId AND p.publico = true
              AND (p.bloqueadaModeracao IS NULL OR p.bloqueadaModeracao = false)
            """)
    List<Playlist> findPublicasVisiveisByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT COUNT(p) FROM Playlist p
            WHERE p.userId = :userId AND p.publico = true
              AND (p.bloqueadaModeracao IS NULL OR p.bloqueadaModeracao = false)
            """)
    long countPublicasVisiveisByUserId(@Param("userId") Long userId);
}
