package com.newwave.student_management.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI studentManagementOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Student Management API")
                        .description("OpenAPI documentation for Student Management service")
                        .version("v1.0.0"));
    }
}

