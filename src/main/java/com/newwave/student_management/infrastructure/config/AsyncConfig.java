package com.newwave.student_management.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Configuration for async operations (e.g., email sending)
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
