package com.synfonia.musicas.repositories;

import com.synfonia.musicas.entities.MusicEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MusicRepository extends JpaRepository<MusicEntity, String> {

    // 1. Busca por nome da música (ignorando maiúsculas/minúsculas)
    List<MusicEntity> findByNomeContainingIgnoreCase(String nome);

    // 2. Busca todas as músicas de um álbum específico
    List<MusicEntity> findByAlbumContainingIgnoreCase(String album);

    // 3. Busca músicas por artista
    List<MusicEntity> findByArtistaContainingIgnoreCase(String artista);

    // 4. Busca músicas por um intervalo de anos (Ex: "Anos 2000")
    List<MusicEntity> findByAnoLancamentoBetween(Integer anoInicio, Integer anoFim);

    // 5. Filtro combinado: nome E artista
    @Query("""
            SELECT m FROM MusicEntity m
            WHERE LOWER(m.nome) LIKE LOWER(CONCAT('%', :nome, '%'))
              AND LOWER(m.artista) LIKE LOWER(CONCAT('%', :artista, '%'))
            """)
    List<MusicEntity> findByNomeAndArtistaCustom(@Param("nome") String nome, @Param("artista") String artista);

    // 6. Busca global: termo em nome, artista OU álbum
    @Query("""
            SELECT m FROM MusicEntity m
            WHERE LOWER(m.nome) LIKE LOWER(CONCAT('%', :termo, '%'))
               OR LOWER(m.artista) LIKE LOWER(CONCAT('%', :termo, '%'))
               OR LOWER(m.album) LIKE LOWER(CONCAT('%', :termo, '%'))
            """)
    List<MusicEntity> searchGlobal(@Param("termo") String termo);
}
