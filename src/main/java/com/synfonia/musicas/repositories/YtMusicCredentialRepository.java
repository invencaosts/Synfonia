package com.synfonia.musicas.repositories;

import com.synfonia.musicas.entities.YtMusicCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface YtMusicCredentialRepository extends JpaRepository<YtMusicCredential, Long> {
}
