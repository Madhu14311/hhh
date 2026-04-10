package com.blisssierra.hrms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration:
 * - Exposes RestTemplate bean (used by AttendanceService to call Python
 * FastAPI)
 * - Serves uploaded face images as static resources (optional — for debugging)
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * RestTemplate for outbound HTTP calls to Python FastAPI.
     * AttendanceService autowires this.
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * Serve uploaded face images at /uploads/faces/...
     * Useful for debugging — open
     * http://localhost:8080/uploads/faces/EMP001/face_1.jpg
     * Remove this in production.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:./uploads/");
    }
}