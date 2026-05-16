package br.leetjourney.smartguard.interfaces.rest;

import br.leetjourney.smartguard.application.dto.auth.AuthRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.shaded.com.github.dockerjava.core.MediaType;

import static org.springframework.mock.http.server.reactive.MockServerHttpRequest.post;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers

class AuthControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("smartguard_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void overrideDataSourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        // Desabilita TimescaleDB nos testes (não disponível no postgres:alpine puro)
        registry.add("spring.flyway.locations", () -> "classpath:db/migration/test");
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /auth/register → 201 com token JWT")
    void registerReturnsToken() throws Exception {
        var request = new AuthRequest.Register(
                "Acme Corp", "acme-corp",
                "Admin User", "admin@acme.com", "senha123456"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.role").value("TENANT_ADMIN"))
                .andExpect(jsonPath("$.email").value("admin@acme.com"));
    }

    @Test
    @DisplayName("POST /auth/register com slug duplicado → 409")
    void registerDuplicateSlugReturnsConflict() throws Exception {
        var request = new AuthRequest.Register(
                "Beta Corp", "beta-corp",
                "Admin", "admin@beta.com", "senha123456"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Segunda tentativa com mesmo slug
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /auth/login → 200 com token JWT")
    void loginReturnsToken() throws Exception {
        // Cria o tenant primeiro
        var registerReq = new AuthRequest.Register(
                "Gamma Corp", "gamma-corp",
                "Admin", "admin@gamma.com", "senha123456"
        );
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerReq)));

        // Faz login
        var loginReq = new AuthRequest.Login("gamma-corp", "admin@gamma.com", "senha123456");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    @DisplayName("POST /auth/login com senha errada → 401")
    void loginWrongPasswordReturns401() throws Exception {
        var loginReq = new AuthRequest.Login("gamma-corp", "admin@gamma.com", "senhaerrada");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /auth/register com email inválido → 422")
    void registerInvalidEmailReturns422() throws Exception {
        var request = new AuthRequest.Register(
                "Delta Corp", "delta-corp",
                "Admin", "nao-e-email", "senha123456"
        );
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors.email").exists());
    }
}
