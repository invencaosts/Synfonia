package com.joaopaulo.musicas.services;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
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
        Bandwidth limit = Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1)));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private Bucket createApiBucket(String key) {
        // Limite: 60 tokens, refil de 60 tokens a cada 1 minuto
        Bandwidth limit = Bandwidth.classic(60, Refill.intervally(60, Duration.ofMinutes(1)));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
