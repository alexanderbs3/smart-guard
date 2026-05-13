package br.leetjourney.smartguard.domain.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


@Slf4j
@Service
@RequiredArgsConstructor

public class RiskScoreService {

    private static final String KEY_PREFIX = "risk:";
    private static final long WINDOW_MINUTES = 60;
    private static final long WINDOW_MS = WINDOW_MINUTES * 60 * 1_000;
    private static final int HIGH_VOLUME_THRESHOLD = 100; // eventos/hora → score 100

    private final StringRedisTemplate redis;

    /**
     * Registra um evento na janela deslizante e retorna o score atualizado (0–100).
     */
    public int recordAndScore(UUID tenantId, String actorId, UUID eventId) {
        String key = buildKey(tenantId, actorId);
        long now = Instant.now().toEpochMilli();

        // Adiciona o evento na janela
        redis.opsForZSet().add(key, eventId.toString(), now);

        // Remove eventos fora da janela deslizante
        redis.opsForZSet().removeRangeByScore(key, 0, now - WINDOW_MS);

        // TTL automático para limpeza de atores inativos
        redis.expire(key, WINDOW_MINUTES * 2, TimeUnit.MINUTES);

        // Conta eventos na janela e normaliza para 0–100
        Long count = redis.opsForZSet().zCard(key);
        return normalizeScore(count != null ? count : 0);
    }

    /**
     * Retorna o score atual do ator sem registrar novo evento.
     */
    public int getCurrentScore(UUID tenantId, String actorId) {
        String key = buildKey(tenantId, actorId);
        long now = Instant.now().toEpochMilli();

        redis.opsForZSet().removeRangeByScore(key, 0, now - WINDOW_MS);

        Long count = redis.opsForZSet().zCard(key);
        return normalizeScore(count != null ? count : 0);
    }

    private int normalizeScore(long eventCount) {
        if (eventCount >= HIGH_VOLUME_THRESHOLD) return 100;
        return (int) ((eventCount * 100) / HIGH_VOLUME_THRESHOLD);
    }

    private String buildKey(UUID tenantId, String actorId) {
        return KEY_PREFIX + tenantId + ":" + actorId;
    }
}
