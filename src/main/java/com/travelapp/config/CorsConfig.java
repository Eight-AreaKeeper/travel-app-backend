package com.travelapp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS configuration. In local development, Vue (Vite defaults to port 5173) and Spring Boot
 * (defaults to port 8080) are two different origins, so cross-origin requests must be allowed,
 * otherwise Axios requests get blocked by the browser.
 * In production, replace allowedOrigins with the actual frontend domain instead of using "*".
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173", "http://localhost:3000") // TODO: replace with the real domain in production
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
