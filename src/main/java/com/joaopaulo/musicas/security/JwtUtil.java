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

    private SecretKey getSigningKey() {
        // Gerar chave a partir do segredo (padrão HS512)
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

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
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
            final Claims claims = extractAllClaims(token);
            final String emailFromToken = claims.getSubject();
            final Date expiration = claims.getExpiration();
            return emailFromToken.equals(email) && !expiration.before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token inválido: {}", e.getMessage());
            return false;
        }
    }

    public boolean isTokenValidGracefully(String token) {
        try {
            final Claims claims = extractAllClaims(token);
            final Date expiration = claims.getExpiration();
            return !expiration.before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
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