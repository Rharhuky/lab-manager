package com.campuslab.shared.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * Filter that processes JWT tokens for authentication.
 * Runs once per request and validates the JWT token from the Authorization header.
 * 
 * If token is valid, sets authentication in the SecurityContext.
 * If token is invalid, allows the request to continue without authentication
 * (letting Spring Security handle 401/403 responses).
 */
@Component
public class SecurityFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public SecurityFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        
        String token = extractToken(request);
        
        if (token != null) {
            try {
                Claims claims = jwtService.validateToken(token);
                String userId = claims.getSubject();
                String role = claims.get("role", String.class);
                
                // Create authentication with role authority
                // Spring Security expects roles to be prefixed with "ROLE_"
                SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        Collections.singletonList(authority)
                    );
                
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException e) {
                // Token validation failed - let the request proceed without authentication
                // Spring Security will handle the 401/403 response based on endpoint rules
            }
        }
        
        filterChain.doFilter(request, response);
    }

    /**
     * Extracts the JWT token from the Authorization header.
     * Returns null if header is missing or doesn't start with "Bearer ".
     */
    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }
        
        return null;
    }
}
