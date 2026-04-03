package com.joaopaulo.musicas.repositories;

import com.joaopaulo.musicas.entities.PasswordResetToken;
import com.joaopaulo.musicas.entities.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);
    
    Optional<PasswordResetToken> findByTokenAndUsuarioEmail(String token, String email);

    void deleteByUsuario(Usuario usuario);
}
