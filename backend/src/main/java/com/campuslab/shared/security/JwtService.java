package com.campuslab.shared.security;

import com.campuslab.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

/**
 * Service responsible for JWT token generation and validation.
 * Uses JJWT library to handle JWT operations with proper security.
 */
@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expiresIn;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiresIn}") long expiresIn
    ) {
        validateExpiresIn(expiresIn);
        this.secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
        this.expiresIn = expiresIn;
    }

    /**
     * Validates that expiresIn is within the allowed range (60-86400 seconds).
     */
    private void validateExpiresIn(long expiresIn) {
        if (expiresIn < 60 || expiresIn > 86400) {
            throw new IllegalArgumentException(
                "JWT expiresIn must be between 60 and 86400 seconds, but was: " + expiresIn
            );
        }
    }

    /**
     * Generates a JWT token for the given user.
     * The token contains: sub (userId), role, iat (issued at), exp (expiration).
     *
     * @param user the user to generate the token for
     * @return the JWT token string
     */
    public String generateToken(User user) {
        if (user == null || user.getId() == null || user.getRole() == null) {
            throw new IllegalArgumentException("User and its id/role cannot be null");
        }

        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(expiresIn);

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Validates the JWT token and returns its claims.
     * Throws JwtException if the token is invalid, expired, or malformed.
     *
     * @param token the JWT token string
     * @return the parsed claims
     * @throws JwtException if token validation fails
     */
    public Claims validateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new JwtException("Token cannot be null or empty");
        }

        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            throw new JwtException("Invalid JWT token", e);
        }
    }

    /**
     * Extracts the user ID from the token.
     *
     * @param token the JWT token string
     * @return the user ID as string
     * @throws JwtException if token is invalid
     */
    public String extractUserId(String token) {
        Claims claims = validateToken(token);
        return claims.getSubject();
    }

    /**
     * Extracts the user role from the token.
     *
     * @param token the JWT token string
     * @return the user role as string
     * @throws JwtException if token is invalid
     */
    public String extractRole(String token) {
        Claims claims = validateToken(token);
        return claims.get("role", String.class);
    }
}