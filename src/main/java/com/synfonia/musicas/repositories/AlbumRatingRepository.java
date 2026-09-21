package com.synfonia.musicas.repositories;

import com.synfonia.musicas.entities.AlbumRating;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlbumRatingRepository extends MongoRepository<AlbumRating, String> {

    Optional<AlbumRating> findByUserIdAndAlbumKey(Long userId, String albumKey);

    List<AlbumRating> findByUserIdOrderByAtualizadoEmDesc(Long userId);
}
