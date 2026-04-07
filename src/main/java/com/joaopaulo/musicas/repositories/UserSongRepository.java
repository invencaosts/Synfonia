package com.joaopaulo.musicas.repositories;

import com.joaopaulo.musicas.entities.UserSong;
import com.joaopaulo.musicas.enums.MusicSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSongRepository extends MongoRepository<UserSong, String> {

    Page<UserSong> findByUserId(Long userId, Pageable pageable);

    @org.springframework.data.mongodb.repository.Query(value = "{ 'userId': ?0, '$or': [ { 'trackName': { $regex: '.*?1.*', $options: 'i' } }, { 'artistName': { $regex: '.*?1.*', $options: 'i' } }, { 'albumName': { $regex: '.*?1.*', $options: 'i' } } ] }")
    Page<UserSong> findByUserIdAndSearchTerm(Long userId, String searchTerm, Pageable pageable);

    List<UserSong> findAllByUserId(Long userId);

    @org.springframework.data.mongodb.repository.Query(value = "{ 'userId': ?0 }", fields = "{ 'trackId': 1, '_id': 0 }")
    List<UserSong> findTrackIdsByUserId(Long userId);

    Optional<UserSong> findByUserIdAndTrackId(Long userId, String trackId);

    void deleteByUserIdAndTrackId(Long userId, String trackId);

    Long deleteByUserIdAndSource(Long userId, MusicSource source);


    boolean existsByUserIdAndTrackId(Long userId, String trackId);

    void deleteAllByUserId(Long userId);
}