package com.blisssierra.hrms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * Global CORS configuration.
 *
 * Why needed:
 * - Expo Go on a phone (LAN IP) → Spring Boot on laptop: different origins
 * - Web browser (localhost:19006) → Spring Boot (localhost:8080): different
 * ports
 * - Both need to be allowed for the full-stack app to work
 *
 * This config allows all origins, methods, and headers so that
 * both mobile (Expo Go) and web (browser) clients can reach Spring Boot.
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // Allow any origin — required for Expo Go on phone (dynamic LAN IPs)
        config.addAllowedOriginPattern("*");

        // Allow all standard HTTP methods
        config.addAllowedMethod("GET");
        config.addAllowedMethod("POST");
        config.addAllowedMethod("PUT");
        config.addAllowedMethod("DELETE");
        config.addAllowedMethod("OPTIONS");
        config.addAllowedMethod("PATCH");

        // Allow all headers (Content-Type, Authorization, multipart, etc.)
        config.addAllowedHeader("*");

        // Allow credentials (not needed here but good practice)
        config.setAllowCredentials(false);

        // Apply to all routes
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}