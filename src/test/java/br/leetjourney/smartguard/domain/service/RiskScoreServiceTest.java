package br.leetjourney.smartguard.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class RiskScoreServiceTest {
    private RiskScoreService riskScoreService;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        // Monta um StringRedisTemplate apontando para Redis local (precisa de Redis rodando ou Testcontainers)
        LettuceConnectionFactory factory = new LettuceConnectionFactory("localhost", 6379);
        factory.afterPropertiesSet();
        StringRedisTemplate template = new StringRedisTemplate(factory);
        template.afterPropertiesSet();

        riskScoreService = new RiskScoreService(template);
        tenantId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Score começa em 0 para ator sem eventos")
    void initialScoreIsZero() {
        int score = riskScoreService.getCurrentScore(tenantId, "actor-novo");
        assertThat(score).isEqualTo(0);
    }

    @Test
    @DisplayName("Score aumenta conforme eventos são registrados")
    void scoreIncreasesWithEvents() {
        String actorId = "actor-" + UUID.randomUUID();
        int score1 = riskScoreService.recordAndScore(tenantId, actorId, UUID.randomUUID());
        int score2 = riskScoreService.recordAndScore(tenantId, actorId, UUID.randomUUID());
        int score3 = riskScoreService.recordAndScore(tenantId, actorId, UUID.randomUUID());

        assertThat(score1).isLessThan(score3);
        assertThat(score2).isLessThan(score3);
    }

    @Test
    @DisplayName("Score máximo é 100 com 100+ eventos na janela")
    void scoreIsMaxAt100Events() {
        String actorId = "actor-" + UUID.randomUUID();
        int lastScore = 0;
        for (int i = 0; i < 100; i++) {
            lastScore = riskScoreService.recordAndScore(tenantId, actorId, UUID.randomUUID());
        }
        assertThat(lastScore).isEqualTo(100);
    }

    @Test
    @DisplayName("Tenants diferentes têm scores isolados")
    void tenantScoresAreIsolated() {
        UUID tenant1 = UUID.randomUUID();
        UUID tenant2 = UUID.randomUUID();
        String actorId = "mesmo-ator";

        for (int i = 0; i < 50; i++) {
            riskScoreService.recordAndScore(tenant1, actorId, UUID.randomUUID());
        }

        int scoreTenant2 = riskScoreService.getCurrentScore(tenant2, actorId);
        assertThat(scoreTenant2).isEqualTo(0);
    }
}
