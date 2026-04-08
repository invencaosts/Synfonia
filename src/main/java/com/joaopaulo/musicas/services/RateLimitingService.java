package com.joaopaulo.musicas.services;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitingService {

    // Cache de buckets por IP
    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> apiBuckets = new ConcurrentHashMap<>();

    /**
     * Retorna o balde para o endpoint de Login (5 req/min)
     */
    public Bucket resolveLoginBucket(String ip) {
        return loginBuckets.computeIfAbsent(ip, this::createLoginBucket);
    }

    /**
     * Retorna o balde para endpoints gerais (60 req/min)
     */
    public Bucket resolveApiBucket(String ip) {
        return apiBuckets.computeIfAbsent(ip, this::createApiBucket);
    }

    private Bucket createLoginBucket(String key) {
        // Limite: 5 tokens, refil de 5 tokens a cada 1 minuto
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(5)
                        .refillIntervally(5, Duration.ofMinutes(1))
                        .build())
                .build();
    }

    private Bucket createApiBucket(String key) {
        // Limite expandido: 2000 tokens de capacidade para bursts (importações)
        // Refill Greedy: Regenera 2000 tokens por minuto continuamente (não espera o minuto virar)
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(2000)
                        .refillGreedy(2000, Duration.ofMinutes(1))
                        .build())
                .build();
    }
}
