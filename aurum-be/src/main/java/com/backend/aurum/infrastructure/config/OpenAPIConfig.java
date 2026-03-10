package com.backend.aurum.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .components(new Components()
                        .addParameters("userIdHeader", new Parameter()
                                .in("header")
                                .name("X-User-Id")
                                .description("Custom header for User Multi-tenancy (UUID)")
                                .required(true)))
                .info(new Info()
                        .title("Aurum API")
                        .version("1.0")
                        .description("Backend API for the Aurum wealth tracking application."));
    }
}
