package com.travelapp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelapp.common.ApiResponse;
import com.travelapp.common.ErrorCode;
import com.travelapp.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration. Permits login/registration/public browsing endpoints; all other
 * endpoints require authentication by default, handled by JwtAuthenticationFilter parsing the
 * Authorization: Bearer {token} header.
 *
 * TODO (future work):
 * When unauthenticated users access the customize Q&A module (/api/v1/customize/**), authenticate
 * with a temporary sessionToken instead; this doesn't go through the standard JWT flow, so it's
 * best handled separately in the customize module's Controller/interceptor.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/**",
            "/doc.html",
            "/webjars/**",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/actuator/health",
            "/uploads/**"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Decoupled frontend/backend using JWT, no CSRF protection needed
                // Apply CorsConfig's rules inside the security chain too, so CORS preflight (OPTIONS) isn't rejected before reaching MVC
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/destinations/**").permitAll()
                        .requestMatchers("/api/v1/customize/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/notes", "/api/v1/notes/*", "/api/v1/notes/*/comments").permitAll()
                        .anyRequest().authenticated()
                )
                // Without these, a missing/expired token gets a bare 403 and the frontend can't tell it should refresh the token.
                // Return 401 + { code: 40100 } so the Axios interceptor refreshes the token or sends the user to log in.
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                writeError(response, HttpServletResponse.SC_UNAUTHORIZED, ErrorCode.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeError(response, HttpServletResponse.SC_FORBIDDEN, ErrorCode.FORBIDDEN))
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private void writeError(HttpServletResponse response, int status, ErrorCode errorCode) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(errorCode.getCode(), errorCode.getDefaultMessage()));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
