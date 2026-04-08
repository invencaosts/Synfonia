package com.joaopaulo.musicas.security;

import com.joaopaulo.musicas.services.RateLimitingService;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimitingService rateLimitingService;

    @Override
    protected void doFilterInternal(
            @org.springframework.lang.NonNull HttpServletRequest request,
            @org.springframework.lang.NonNull HttpServletResponse response,
            @org.springframework.lang.NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Ignorar requisições OPTIONS (Preflight de CORS) para não consumir rate limit
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = getClientIP(request);
        String path = request.getRequestURI();

        Bucket bucket;
        // Se for login ou registro, usamos o bucket restrito
        if (path.contains("/auth/login") || path.contains("/auth/register")) {
            bucket = rateLimitingService.resolveLoginBucket(ip);
        } else {
            // Outros endpoints usam o bucket geral
            bucket = rateLimitingService.resolveApiBucket(ip);
        }

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            // Permitido, adiciona header com tokens restantes para transparência
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
        } else {
            // Limite excedido
            long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;
            log.warn("Rate limit excedido para IP: {} no path: {}. Esperar {} segundos.", ip, path, waitForRefill);
            
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitForRefill));
            
            String jsonError = String.format(
                "{\"status\": 429, \"erro\": \"Muitas requisições\", \"detalhe\": \"Limite de tentativas excedido. Tente novamente em %d segundos.\"}",
                waitForRefill
            );
            response.getWriter().write(jsonError);
        }
    }

    private String getClientIP(HttpServletRequest request) {
        // Para evitar Spoofing, usamos o RemoteAddr real da conexão.
        // Se a aplicação estiver atrás de um proxy reverso confiável (Nginx/Cloudflare),
        // o Spring deve ser configurado via 'server.forward-headers-strategy=native'
        // para que o getRemoteAddr() já retorne o IP correto do cliente de forma segura.
        return request.getRemoteAddr();
    }
}
