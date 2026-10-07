package com.synfonia.musicas.repositories;

import com.synfonia.musicas.entities.UserSong;
import com.synfonia.musicas.enums.MusicSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSongRepository extends JpaRepository<UserSong, String> {

    Page<UserSong> findByUserId(Long userId, Pageable pageable);

    @Query("""
            SELECT us FROM UserSong us
            WHERE us.userId = :userId
              AND (LOWER(us.trackName) LIKE LOWER(CONCAT('%', :termo, '%'))
                   OR LOWER(us.artistName) LIKE LOWER(CONCAT('%', :termo, '%'))
                   OR LOWER(us.albumName) LIKE LOWER(CONCAT('%', :termo, '%')))
            """)
    Page<UserSong> findByUserIdAndSearchTerm(@Param("userId") Long userId, @Param("termo") String searchTerm, Pageable pageable);

    Optional<UserSong> findByUserIdAndTrackId(Long userId, String trackId);

    @Transactional
    @Modifying
    @Query("DELETE FROM UserSong us WHERE us.userId = :userId AND us.trackId = :trackId")
    void deleteByUserIdAndTrackId(@Param("userId") Long userId, @Param("trackId") String trackId);

    @Transactional
    @Modifying
    @Query("DELETE FROM UserSong us WHERE us.userId = :userId AND us.source = :source")
    int deleteByUserIdAndSource(@Param("userId") Long userId, @Param("source") MusicSource source);

    boolean existsByUserIdAndTrackId(Long userId, String trackId);

    @Query("SELECT us.trackId FROM UserSong us WHERE us.userId = :userId")
    List<String> findTrackIdsByUserId(@Param("userId") Long userId);

    @Query("SELECT us.trackId FROM UserSong us WHERE us.userId = :userId AND (us.source IS NULL OR us.source <> :source)")
    List<String> findTrackIdsByUserIdAndSourceNot(@Param("userId") Long userId, @Param("source") MusicSource source);

    long countByUserId(Long userId);
}
