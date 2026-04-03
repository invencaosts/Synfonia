package com.joaopaulo.musicas.services;

import com.joaopaulo.musicas.entities.PasswordResetToken;
import com.joaopaulo.musicas.entities.Usuario;
import com.joaopaulo.musicas.exceptions.UsuarioNaoEncontradoException;
import com.joaopaulo.musicas.repositories.PasswordResetTokenRepository;
import com.joaopaulo.musicas.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import java.util.Random;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void createPasswordResetToken(String email) {
        String normalizedEmail = email.trim().toLowerCase();
        Usuario usuario = usuarioRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Nenhum usuário encontrado com este e-mail."));

        // Limpa tokens antigos do usuário
        tokenRepository.deleteByUsuario(usuario);
        tokenRepository.flush(); 

        // Gera código de 6 dígitos
        String code = String.format("%06d", new Random().nextInt(1000000));
        
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(code)
                .usuario(usuario)
                .dataExpiracao(LocalDateTime.now().plusMinutes(15))
                .build();
        
        tokenRepository.save(resetToken);
        tokenRepository.flush(); // Garante que o token esteja no banco antes de enviar o e-mail

        try {
            emailService.sendResetPasswordEmail(usuario.getEmail(), code);
            System.out.println("[DEBUG] Código gerado para " + normalizedEmail + ": " + code);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao enviar e-mail de recuperação", e);
        }
    }

    @Transactional(readOnly = true)
    public void verifyResetCode(String email, String code) {
        String normalizedEmail = email.trim().toLowerCase();
        String normalizedCode = code.trim();

        System.out.println("[DEBUG] Tentativa de verificação: " + normalizedEmail + " | " + normalizedCode);

        PasswordResetToken resetToken = tokenRepository.findByTokenAndUsuarioEmail(normalizedCode, normalizedEmail)
                .orElseThrow(() -> {
                    System.out.println("[DEBUG] Código/E-mail não encontrados no banco para: " + normalizedCode);
                    // Log adicional para ver o que tem no banco
                    tokenRepository.findAll().forEach(t -> 
                        System.out.println("[DEBUG] Token no banco: " + t.getToken() + " p/ " + t.getUsuario().getEmail())
                    );
                    return new RuntimeException("Código de recuperação inválido ou e-mail não confere");
                });

        if (resetToken.isExpirado()) {
            System.out.println("[DEBUG] Código expirado: " + normalizedCode);
            throw new RuntimeException("Código de recuperação expirado");
        }
        
        System.out.println("[DEBUG] Verificação bem-sucedida para: " + normalizedEmail);
    }

    @Transactional
    public void resetPassword(String email, String code, String newPassword) {
        String normalizedEmail = email.trim().toLowerCase();
        String normalizedCode = code.trim();

        PasswordResetToken resetToken = tokenRepository.findByTokenAndUsuarioEmail(normalizedCode, normalizedEmail)
                .orElseThrow(() -> new RuntimeException("Código de recuperação inválido ou expirado"));

        if (resetToken.isExpirado()) {
            throw new RuntimeException("Código de recuperação expirado");
        }

        Usuario usuario = resetToken.getUsuario();
        usuario.setSenha(passwordEncoder.encode(newPassword));
        usuarioRepository.save(usuario);

        // Remove o token após o uso
        tokenRepository.delete(resetToken);
    }
}
