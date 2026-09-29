package com.campuslab.shared.config;

import com.campuslab.shared.dto.ErrorResponse;
import com.campuslab.shared.security.SecurityFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.Instant;

/**
 * Spring Security configuration.
 * Configures stateless JWT-based authentication with role-based access control.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final SecurityFilter securityFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(SecurityFilter securityFilter, ObjectMapper objectMapper) {
        this.securityFilter = securityFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                // Read access for both ALUNO and PROFESSOR
                .requestMatchers(HttpMethod.GET, "/api/laboratories/**").hasAnyRole("ALUNO", "PROFESSOR")
                .requestMatchers(HttpMethod.GET, "/api/reservations/**").hasAnyRole("ALUNO", "PROFESSOR")
                // Write access only for PROFESSOR
                .requestMatchers("/api/laboratories/**").hasRole("PROFESSOR")
                .requestMatchers("/api/reservations/**").hasRole("PROFESSOR")
                // Any other authenticated
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    ErrorResponse errorResponse = new ErrorResponse(
                        Instant.now(),
                        HttpStatus.UNAUTHORIZED.value(),
                        "AUTHENTICATION_REQUIRED",
                        "Autenticação é necessária para acessar este recurso",
                        request.getRequestURI()
                    );
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    ErrorResponse errorResponse = new ErrorResponse(
                        Instant.now(),
                        HttpStatus.FORBIDDEN.value(),
                        "INSUFFICIENT_PERMISSIONS",
                        "Você não tem permissão para acessar este recurso",
                        request.getRequestURI()
                    );
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                })
            )
            .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
