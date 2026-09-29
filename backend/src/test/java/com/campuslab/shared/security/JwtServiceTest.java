package com.campuslab.shared.security;

import com.campuslab.user.User;
import com.campuslab.user.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private User testUser;

    @BeforeEach
    void setUp() {
        String testSecret = "dGVzdFNlY3JldEtleUZvckp3dFNlcnZpY2VUZXN0czMyQnl0ZXNLZXk="; // base64 encoded test key
        long testExpiresIn = 3600; // 1 hour
        
        jwtService = new JwtService(testSecret, testExpiresIn);
        
        testUser = new User("Test User", "test@example.com", "password", UserRole.PROFESSOR);
        ReflectionTestUtils.setField(testUser, "id", UUID.randomUUID());
    }

    @Test
    void generateToken_shouldCreateValidToken() {
        // When
        String token = jwtService.generateToken(testUser);

        // Then
        assertThat(token).isNotNull().isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts separated by dots
    }

    @Test
    void validateToken_shouldReturnClaimsForValidToken() {
        // Given
        String token = jwtService.generateToken(testUser);

        // When
        Claims claims = jwtService.validateToken(token);

        // Then
        assertThat(claims).isNotNull();
        assertThat(claims.getSubject()).isEqualTo(testUser.getId().toString());
        assertThat(claims.get("role", String.class)).isEqualTo(testUser.getRole().name());
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isNotNull();
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    void validateToken_shouldThrowExceptionForInvalidToken() {
        // When & Then
        assertThatThrownBy(() -> jwtService.validateToken("invalid.token.here"))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("Invalid JWT token");
    }

    @Test
    void validateToken_shouldThrowExceptionForNullToken() {
        // When & Then
        assertThatThrownBy(() -> jwtService.validateToken(null))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("Token cannot be null or empty");
    }

    @Test
    void validateToken_shouldThrowExceptionForEmptyToken() {
        // When & Then
        assertThatThrownBy(() -> jwtService.validateToken("   "))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("Token cannot be null or empty");
    }

    @Test
    void extractUserId_shouldReturnCorrectUserId() {
        // Given
        String token = jwtService.generateToken(testUser);

        // When
        String extractedUserId = jwtService.extractUserId(token);

        // Then
        assertThat(extractedUserId).isEqualTo(testUser.getId().toString());
    }

    @Test
    void extractRole_shouldReturnCorrectRole() {
        // Given
        String token = jwtService.generateToken(testUser);

        // When
        String extractedRole = jwtService.extractRole(token);

        // Then
        assertThat(extractedRole).isEqualTo(testUser.getRole().name());
    }

    @Test
    void generateToken_shouldThrowExceptionForNullUser() {
        // When & Then
        assertThatThrownBy(() -> jwtService.generateToken(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User and its id/role cannot be null");
    }

    @Test
    void generateToken_shouldThrowExceptionForUserWithNullId() {
        // Given
        User userWithoutId = new User("Test", "test@example.com", "password", UserRole.ALUNO);

        // When & Then
        assertThatThrownBy(() -> jwtService.generateToken(userWithoutId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User and its id/role cannot be null");
    }

    @Test
    void generateToken_shouldThrowExceptionForUserWithNullRole() {
        // Given
        User userWithoutRole = new User("Test", "test@example.com", "password", null);
        ReflectionTestUtils.setField(userWithoutRole, "id", UUID.randomUUID());

        // When & Then
        assertThatThrownBy(() -> jwtService.generateToken(userWithoutRole))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User and its id/role cannot be null");
    }

    @Test
    void constructor_shouldThrowExceptionForInvalidExpiresInTooLow() {
        // When & Then
        assertThatThrownBy(() -> new JwtService("dGVzdFNlY3JldEtleUZvckp3dFNlcnZpY2VUZXN0czMyQnl0ZXNLZXk=", 59))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JWT expiresIn must be between 60 and 86400 seconds, but was: 59");
    }

    @Test
    void constructor_shouldThrowExceptionForInvalidExpiresInTooHigh() {
        // When & Then
        assertThatThrownBy(() -> new JwtService("dGVzdFNlY3JldEtleUZvckp3dFNlcnZpY2VUZXN0czMyQnl0ZXNLZXk=", 86401))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JWT expiresIn must be between 60 and 86400 seconds, but was: 86401");
    }

    @Test
    void constructor_shouldAcceptValidExpiresInBoundaryValues() {
        // Should not throw exceptions for boundary values
        assertThatCode(() -> new JwtService("dGVzdFNlY3JldEtleUZvckp3dFNlcnZpY2VUZXN0czMyQnl0ZXNLZXk=", 60))
                .doesNotThrowAnyException();
        
        assertThatCode(() -> new JwtService("dGVzdFNlY3JldEtleUZvckp3dFNlcnZpY2VUZXN0czMyQnl0ZXNLZXk=", 86400))
                .doesNotThrowAnyException();
    }

    @Test
    void tokensShouldContainCorrectClaims() {
        // Given
        String token = jwtService.generateToken(testUser);
        Claims claims = jwtService.validateToken(token);

        // Then - verify all required claims are present
        assertThat(claims.getSubject()).isNotNull().isEqualTo(testUser.getId().toString());
        assertThat(claims.get("role")).isNotNull().isEqualTo(testUser.getRole().name());
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isNotNull();
        
        // Verify expiration is set correctly (within 1 second tolerance)
        Instant expectedExp = claims.getIssuedAt().toInstant().plusSeconds(3600);
        Instant actualExp = claims.getExpiration().toInstant();
        assertThat(actualExp).isBetween(
            expectedExp.minusSeconds(1),
            expectedExp.plusSeconds(1)
        );
    }
}