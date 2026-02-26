package com.gembud.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger configuration for API documentation.
 *
 * @author Gembud Team
 * @since 2026-02-22
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI gembudOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Gembud API")
                .description("Game matching platform API - 게임 매칭 플랫폼 API")
                .version("v1.0.0"))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
            .components(new Components()
                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")));
    }
}
