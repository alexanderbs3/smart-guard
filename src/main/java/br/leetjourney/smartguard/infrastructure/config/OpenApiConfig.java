package br.leetjourney.smartguard.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI smartGuardOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SmartGuard API")
                        .description("Behavioral Audit & Anomaly Detection Platform")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("SmartGuard Engineering")
                                .url("https://github.com/seu-usuario/smartguard")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Cole o token JWT retornado pelo /auth/login")));
    }
}
