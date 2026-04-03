package com.joaopaulo.musicas.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // Ignorar requisições OPTIONS (Preflight de CORS)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = null;

        // 1. Tentar extrair do Header (Prioridade para APIs/Mobile)
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String headerToken = authHeader.substring(7);
            if (jwtUtil.isTokenValidGracefully(headerToken)) {
                token = headerToken;
            }
        }

        // 2. Se não houver token válido no Header, tentar extrair do Cookie (Web Session)
        if (token == null && request.getCookies() != null) {
            token = Arrays.stream(request.getCookies())
                    .filter(cookie -> "accessToken".equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String email = jwtUtil.extractEmail(token);
            boolean isNotAuthenticated = SecurityContextHolder.getContext().getAuthentication() == null;

            if (email != null && isNotAuthenticated) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                if (jwtUtil.isTokenValid(token, email)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token JWT inválido ou malformado: {}", e.getMessage());
            // Não bloqueamos aqui para permitir que rotas públicas funcionem mesmo com token inválido
            // O SecurityConfig cuida da autorização final
        }

        filterChain.doFilter(request, response);
    }
}
