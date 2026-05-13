package br.leetjourney.smartguard.interfaces.security;

import br.leetjourney.smartguard.domain.model.Tenant;
import br.leetjourney.smartguard.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {
    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        // Secret com 64+ chars para HS256
        String secret = "smartguard-test-secret-must-be-long-enough-for-hs256-at-least-64chars!!";
        jwtService = new JwtService(secret, 3_600_000L);

        Tenant tenant = Tenant.builder()
                .id(UUID.randomUUID())
                .name("Test Corp")
                .slug("test-corp")
                .build();

        user = User.builder()
                .id(UUID.randomUUID())
                .tenant(tenant)
                .email("user@test.com")
                .fullName("Test User")
                .role(User.UserRole.ANALYST)
                .build();
    }

    @Test
    @DisplayName("Token gerado deve ser válido")
    void generatedTokenIsValid() {
        String token = jwtService.generateToken(user);
        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    @DisplayName("Token deve conter userId e tenantId corretos")
    void tokenContainsCorrectClaims() {
        String token = jwtService.generateToken(user);

        assertThat(jwtService.extractUserId(token)).isEqualTo(user.getId());
        assertThat(jwtService.extractTenantId(token)).isEqualTo(user.getTenant().getId());
    }

    @Test
    @DisplayName("Token adulterado deve ser inválido")
    void tamperedTokenIsInvalid() {
        String token = jwtService.generateToken(user);
        String tampered = token + "adulterado";
        assertThat(jwtService.isTokenValid(tampered)).isFalse();
    }

    @Test
    @DisplayName("Token expirado deve ser inválido")
    void expiredTokenIsInvalid() {
        JwtService shortLived = new JwtService(
                "smartguard-test-secret-must-be-long-enough-for-hs256-at-least-64chars!!",
                1L // 1ms — expira imediatamente
        );
        String token = shortLived.generateToken(user);
        assertThat(shortLived.isTokenValid(token)).isFalse();
    }
}
