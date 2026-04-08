package com.joaopaulo.musicas.security;

import com.joaopaulo.musicas.entities.Usuario;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration.ms}")
    private long accessTokenExpiration; // em milissegundos

    @Value("${jwt.refreshExpiration.ms}")
    private long refreshTokenExpiration;

    @jakarta.annotation.PostConstruct
    public void validateConfig() {
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalStateException("[JWT] ERRO CRÍTICO: A variável JWT_SECRET não foi configurada no ambiente!");
        }
        if (secret.length() < 32) {
            throw new IllegalStateException("[JWT] ERRO CRÍTICO: O segredo configurado em JWT_SECRET é muito curto. Deve ter pelo menos 32 caracteres para garantir a segurança da assinatura HS256.");
        }
        log.info("[JWT] Configuração de segurança validada com sucesso.");
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(Usuario usuario) {
        return generateAccessToken(usuario.getEmail(), usuario.getId(), usuario.getPapel().name());
    }

    public String generateAccessToken(String email, Long userId, String role) {
        return generateToken(email, userId, role, accessTokenExpiration);
    }

    public String generateRefreshToken(Usuario usuario) {
        return generateToken(usuario.getEmail(), usuario.getId(), usuario.getPapel().name(), refreshTokenExpiration);
    }

    private String generateToken(String email, Long userId, String role, long expiration) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("email", email);
        claims.put("role", role);

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        log.info("[JWT] Gerando token para {}. Criado em: {}, Expira em: {}", email, now, expiryDate);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isTokenValid(String token, String email) {
        try {
            final String username = extractEmail(token);
            boolean isValid = username.equals(email) && !isTokenExpired(token);
            if (!isValid) {
                log.warn("[JWT] Token inválido para o usuário {}. Expirado: {}", email, isTokenExpired(token));
            }
            return isValid;
        } catch (Exception e) {
            log.error("[JWT] Erro ao validar token para {}: {}", email, e.getMessage());
            return false;
        }
    }

    public boolean isTokenValidGracefully(String token) {
        if (token == null || token.isEmpty()) return false;
        try {
            Claims claims = extractAllClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return false;
        } catch (io.jsonwebtoken.JwtException e) {
            log.error("[JWT] Erro de SEGURANÇA ou ASSINATURA: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    public Long extractUserId(String token) {
        return extractAllClaims(token).get("userId", Long.class);
    }

    public boolean isTokenExpired(String token) {
        try {
            return extractAllClaims(token).getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return true;
        }
    }

    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }
}