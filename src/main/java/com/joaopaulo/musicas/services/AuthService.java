package com.joaopaulo.musicas.services;

import com.joaopaulo.musicas.dtos.request.LoginRequest;
import com.joaopaulo.musicas.dtos.request.UsuarioRequest;
import com.joaopaulo.musicas.dtos.response.LoginResponse;
import com.joaopaulo.musicas.dtos.response.UsuarioResponse;
import com.joaopaulo.musicas.entities.Usuario;
import com.joaopaulo.musicas.exceptions.CredenciaisInvalidasException;
import com.joaopaulo.musicas.exceptions.EmailJaCadastradoException;
import com.joaopaulo.musicas.exceptions.SenhaInvalidaException;
import com.joaopaulo.musicas.exceptions.UsuarioBloqueadoException;
import com.joaopaulo.musicas.exceptions.UsuarioNaoEncontradoException;
import com.joaopaulo.musicas.mappers.UsuarioMapper;
import com.joaopaulo.musicas.repositories.UsuarioRepository;
import com.joaopaulo.musicas.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UsuarioMapper usuarioMapper;

    @Value("${spring.security.user.name}")
    private String adminEmail;

    @Value("${ADMIN_PASSWORD:#{null}}")
    private String adminPassword;

    public UsuarioResponse register(UsuarioRequest request) {
        log.info("Registro de novo usuário: {}", request.getEmail());

        if (!request.getEmail().equals(request.getConfirmarEmail())) {
            throw new SenhaInvalidaException("E-mail e confirmação de e-mail não conferem");
        }

        if (!request.getSenha().equals(request.getConfirmarSenha())) {
            throw new SenhaInvalidaException("Senha e confirmação de senha não conferem");
        }

        validatePassword(request.getSenha(), request.getEmail());

        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new EmailJaCadastradoException("Este e-mail já está em uso por outra conta");
        }

        if (usuarioRepository.existsByUsername(request.getUsername())) {
            throw new EmailJaCadastradoException("Este nome de usuário já está em uso");
        }

        Usuario usuario = usuarioMapper.toEntity(request);
        usuario.setSenha(passwordEncoder.encode(request.getSenha()));
        usuario.setUsername(request.getUsername());
        usuario.setDisplayName(request.getDisplayName()); 
        usuario.setAtivo(true);
        usuario.setPapel(Usuario.Papel.USER);

        if (adminEmail != null && adminEmail.equals(request.getEmail())) {
            usuario.setPapel(Usuario.Papel.ADMIN);
        }

        Usuario usuarioSalvo = usuarioRepository.save(usuario);
        return usuarioMapper.toResponse(usuarioSalvo);
    }

    public LoginResponse login(LoginRequest request) {
        log.info("Tentativa de login: {}", request.getEmail());

        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário não encontrado ou inativo"));

        if (!usuario.isAtivo()) {
            if (usuario.getDataDesativacao() != null && usuario.getDataDesativacao().plusDays(7).isAfter(LocalDateTime.now())) {
                usuario.setAtivo(true);
                usuario.setDataDesativacao(null);
                usuarioRepository.save(usuario);
                log.info("Conta reativada via login: {}", request.getEmail());
            } else {
                throw new UsuarioNaoEncontradoException("Usuário não encontrado ou conta deletada permanentemente");
            }
        }

        // Verificar bloqueio
        if (usuario.getBloqueadoAte() != null && usuario.getBloqueadoAte().isAfter(LocalDateTime.now())) {
            long secondsRemaining = ChronoUnit.SECONDS.between(LocalDateTime.now(), usuario.getBloqueadoAte());
            throw new UsuarioBloqueadoException("Usuário bloqueado por " + secondsRemaining + " segundos");
        }

        // Validar senha
        if (!passwordEncoder.matches(request.getSenha(), usuario.getSenha())) {
            // Incrementar tentativas falhas e aplicar bloqueio progressivo
            incrementarTentativasEBloquear(usuario);
            throw new CredenciaisInvalidasException();
        }

        // Login bem-sucedido: resetar tentativas
        usuario.setTentativasFalhas(0);
        usuario.setBloqueadoAte(null);
        usuario.setUltimoLogin(LocalDateTime.now());
        usuarioRepository.save(usuario);

        // Gerar tokens
        String accessToken = jwtUtil.generateAccessToken(usuario);
        String refreshToken = jwtUtil.generateRefreshToken(usuario);

        return LoginResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .usuario(usuarioMapper.toResponse(usuario))
                .build();
    }

    @SuppressWarnings("null")
    public UsuarioResponse getAuthenticatedUser() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof com.joaopaulo.musicas.security.UsuarioDetails details)) {
            throw new com.joaopaulo.musicas.exceptions.UnauthorizedException("Usuário não está autenticado");
        }
        
        Usuario usuario = usuarioRepository.findById(details.getId())
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário não encontrado"));
        
        return usuarioMapper.toResponse(usuario);
    }

    private void incrementarTentativasEBloquear(Usuario usuario) {
        int tentativas = Optional.ofNullable(usuario.getTentativasFalhas()).orElse(0) + 1;
        usuario.setTentativasFalhas(tentativas);

        LocalDateTime now = LocalDateTime.now();
        switch (tentativas) {
            case 2: // 2ª falha: 30 segundos
                usuario.setBloqueadoAte(now.plusSeconds(30));
                break;
            case 3: // 3ª falha: 5 minutos
                usuario.setBloqueadoAte(now.plusMinutes(5));
                break;
            case 4: // 4ª falha: 1 hora
                usuario.setBloqueadoAte(now.plusHours(1));
                break;
            default: // 5ª+ falha: 1 dia (máximo)
                if (tentativas >= 5) {
                    usuario.setBloqueadoAte(now.plusDays(1));
                }
                break;
        }

        usuarioRepository.save(usuario);
        log.info("Usuário {} bloqueado por {} tentativas falhas. Novo bloqueio até: {}", 
                usuario.getEmail(), tentativas, usuario.getBloqueadoAte());
    }

    @SuppressWarnings("null")
    private void validatePassword(String password, String email) {
        // Mínimo 8 caracteres
        if (password.length() < 8) {
            throw new SenhaInvalidaException("Senha deve ter pelo menos 8 caracteres");
        }

        // Verifica se contém letras maiúsculas, minúsculas, números e caracteres especiais
        if (!Pattern.compile("[A-Z]").matcher(password).find() ||
            !Pattern.compile("[a-z]").matcher(password).find() ||
            !Pattern.compile("\\d").matcher(password).find() ||
            !Pattern.compile("[!@#$%^&*(),.?\":{}|<>]").matcher(password).find()) {
            throw new SenhaInvalidaException("Senha deve conter letras maiúsculas, minúsculas, números e caracteres especiais");
        }

        // Não pode conter sequências de 3+ caracteres do email ou nome
        if (email != null && email.length() >= 3) {
            for (int i = 0; i <= email.length() - 3; i++) {
                String substring = email.substring(i, i + 3);
                if (password.toLowerCase().contains(substring.toLowerCase())) {
                    throw new SenhaInvalidaException("Senha não pode conter sequências do email");
                }
            }
        }
    }
}