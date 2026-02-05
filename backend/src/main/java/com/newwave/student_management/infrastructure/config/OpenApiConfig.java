package com.newwave.student_management.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI studentManagementOpenAPI() {
        return new OpenAPI()
                .servers(List.of(
                    new Server().url("https://api.admin-datlt244.io.vn/api/v1")
                    .description("Production server"),
                    new Server().url("http://localhost:6868/api/v1")
                    .description("Development server")
                ))
                .info(new Info()
                        .title("Student Management API")
                        .description("OpenAPI documentation for Student Management service")
                        .version("v1.0.0"));
    }
}

