package com.synfonia.musicas.repositories;

import com.synfonia.musicas.entities.Playlist;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlaylistRepository extends MongoRepository<Playlist, String> {
    // Busca todas as playlists de um usuário
    List<Playlist> findByUserId(Long userId);

    // Busca apenas as playlists públicas de um usuário (para o perfil)
    List<Playlist> findByUserIdAndPublicoTrue(Long userId);
}
