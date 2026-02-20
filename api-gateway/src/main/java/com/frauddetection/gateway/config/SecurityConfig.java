package com.frauddetection.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Reactive Spring Security config for the API Gateway.
 *
 * Strategy:
 * - All traffic passes through the gateway → validate JWT Bearer token here.
 * - Downstream microservices run on a trusted internal network (no auth check
 * needed).
 * - Actuator health endpoint is always public (used by Docker health checks).
 * - Preflight OPTIONS requests are always permitted (CORS handling).
 * - Google JWKS URI is configured via application.yml (issuer-uri).
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)

                .authorizeExchange(exchange -> exchange
                        // Always allow health checks and OPTIONS (CORS preflight)
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers("/actuator/health", "/actuator/info").permitAll()
                        // All other routes require a valid JWT Bearer token
                        .anyExchange().authenticated())

                // Configure as OAuth2 Resource Server with JWT (Google OIDC)
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> {
                            // jwk-set-uri / issuer-uri configured in application.yml
                        }))

                .build();
    }
}
