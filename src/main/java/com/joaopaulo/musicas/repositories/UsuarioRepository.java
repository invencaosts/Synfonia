package com.joaopaulo.musicas.repositories;

import com.joaopaulo.musicas.entities.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    Optional<Usuario> findByEmailAndAtivoTrue(String email);

    void deleteByAtivoFalseAndDataDesativacaoBefore(java.time.LocalDateTime date);
}