package com.synfonia.musicas.repositories;

import com.synfonia.musicas.entities.MusicEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MusicRepository extends MongoRepository<MusicEntity, String> {

    // 1. Busca por nome da música (ignorando maiúsculas/minúsculas)
    List<MusicEntity> findByNomeContainingIgnoreCase(String nome);

    // 2. Busca todas as músicas de um álbum específico
    List<MusicEntity> findByAlbumContainingIgnoreCase(String album);

    // 3. Busca músicas por artista
    List<MusicEntity> findByArtistaContainingIgnoreCase(String artista);

    // 4. Busca músicas por um intervalo de anos (Ex: "Anos 2000")
    List<MusicEntity> findByAnoLancamentoBetween(Integer anoInicio, Integer anoFim);

    // 5. Query Customizada: Busca músicas que tenham o nome E o artista (Filtro Combinado)
    @Query("{ 'nome': { $regex: ?0, $options: 'i' }, 'artista': { $regex: ?1, $options: 'i' } }")
    List<MusicEntity> findByNomeAndArtistaCustom(String nome, String artista);

    // 6. Busca Global: Busca o termo em nome, artista OU álbum
    @Query("{ $or: [ " +
           "{ 'nome': { $regex: ?0, $options: 'i' } }, " +
           "{ 'artista': { $regex: ?0, $options: 'i' } }, " +
           "{ 'album': { $regex: ?0, $options: 'i' } } " +
           "] }")
    List<MusicEntity> searchGlobal(String term);
}