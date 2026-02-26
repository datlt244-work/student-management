package com.newwave.student_management.infrastructure.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import javax.crypto.spec.SecretKeySpec;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        @Value("${spring.security.jwt.signer-key}")
        private String signerKey;

        private final JwtBlacklistValidator jwtBlacklistValidator;
        private final JwtTokenVersionValidator jwtTokenVersionValidator;

        private final String[] PUBLIC_POST_ENDPOINTS = {
                        "/auth/login",
                        "/auth/logout",
                        "/auth/forgot-password",
                        "/auth/reset-password",
                        "/auth/refresh-token"
        };

        private final String[] PUBLIC_GET_ENDPOINTS = {
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/auth/activate"
        };

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(AbstractHttpConfigurer::disable)
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .authorizeHttpRequests(request -> request
                                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                                .requestMatchers(HttpMethod.POST, PUBLIC_POST_ENDPOINTS).permitAll()
                                                .requestMatchers(HttpMethod.GET, PUBLIC_GET_ENDPOINTS).permitAll()
                                                .anyRequest().authenticated())
                                .oauth2ResourceServer(oath2 -> oath2
                                                .jwt(jwt -> jwt
                                                                .decoder(jwtDecoder())
                                                                .jwtAuthenticationConverter(
                                                                                jwtAuthenticationConverter())));
                return http.build();
        }

        @Bean
        public JwtDecoder jwtDecoder() {
                SecretKeySpec spec = new SecretKeySpec(signerKey.getBytes(), "HmacSHA256");
                NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(spec)
                                .macAlgorithm(MacAlgorithm.HS256)
                                .build();

                // Default validators (exp, nbf, etc.) + blacklist (jti) + token version
                // (invalidate after password change)
                decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                                JwtValidators.createDefault(),
                                jwtBlacklistValidator,
                                jwtTokenVersionValidator));

                return decoder;
        }

        @Bean
        public JwtAuthenticationConverter jwtAuthenticationConverter() {
                JwtGrantedAuthoritiesConverter converter = new JwtGrantedAuthoritiesConverter();
                converter.setAuthorityPrefix("ROLE_");
                converter.setAuthoritiesClaimName("role");

                JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
                jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(converter);
                return jwtAuthenticationConverter;
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        @Order(Ordered.HIGHEST_PRECEDENCE)
        public CorsFilter corsFilter() {
                CorsConfiguration configuration = new CorsConfiguration();

                // 1. Cho phép các origin cụ thể (Frontend)
                configuration.setAllowedOrigins(java.util.List.of(
                                "https://admin-datlt244.io.vn",
                                "http://admin-datlt244.io.vn",
                                "https://api.admin-datlt244.io.vn", // Backup nếu gọi trực tiếp
                                "http://localhost:5173",
                                "http://localhost:3000"));

                // 2. Cho phép các pattern (Localhost)
                configuration.setAllowedOriginPatterns(java.util.List.of(
                                "http://localhost:*",
                                "https://localhost:*"));

                // 3. Cho phép tất cả các Method phổ biến
                configuration.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

                // 4. Cho phép các Header cần thiết
                configuration.setAllowedHeaders(java.util.List.of(
                                "Authorization",
                                "Content-Type",
                                "X-Requested-With",
                                "Accept",
                                "Origin",
                                "Access-Control-Request-Method",
                                "Access-Control-Request-Headers"));

                // 5. Cho phép gửi kèm Credentials (Token, Cookies)
                configuration.setAllowCredentials(true);

                // 6. Cache kết quả Preflight trong 1 giờ (3600s)
                configuration.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return new CorsFilter(source);
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                return source;
        }

}
