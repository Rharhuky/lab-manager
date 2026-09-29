package com.campuslab.shared.security;

import com.campuslab.user.User;
import com.campuslab.user.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration test for JwtService using Spring Boot test context
 * but without requiring external dependencies like Docker.
 */
@SpringBootTest(classes = {JwtService.class})
@TestPropertySource(properties = {
    "jwt.secret=dGVzdFNlY3JldEtleUZvckp3dFNlcnZpY2VUZXN0czMyQnl0ZXNLZXk=", 
    "jwt.expiresIn=3600"
})
@ActiveProfiles("test")
class JwtServiceIntegrationTest {

    @Autowired
    private JwtService jwtService;

    @Test
    void jwtService_shouldBeProperlyConfigured() {
        assertThat(jwtService).isNotNull();
    }

    @Test
    void fullWorkflow_shouldGenerateAndValidateTokenCorrectly() {
        // Given - Create a test user with all required fields
        User user = new User("João Silva", "joao.silva@campus.edu.br", "hashedPassword", UserRole.PROFESSOR);
        ReflectionTestUtils.setField(user, "id", UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));

        // When - Generate token
        String token = jwtService.generateToken(user);

        // Then - Token should be valid and contain correct claims
        assertThat(token).isNotNull().isNotEmpty();
        
        // Validate token structure
        String[] tokenParts = token.split("\\.");
        assertThat(tokenParts).hasSize(3); // JWT has header.payload.signature
        
        // Validate claims through direct validation
        Claims claims = jwtService.validateToken(token);
        assertThat(claims.getSubject()).isEqualTo("550e8400-e29b-41d4-a716-446655440000");
        assertThat(claims.get("role", String.class)).isEqualTo("PROFESSOR");
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isNotNull();
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
        
        // Validate helper extraction methods
        assertThat(jwtService.extractUserId(token)).isEqualTo("550e8400-e29b-41d4-a716-446655440000");
        assertThat(jwtService.extractRole(token)).isEqualTo("PROFESSOR");
    }

    @Test
    void tokenGeneration_shouldWorkWithAlunoRole() {
        // Given
        User aluno = new User("Maria Santos", "maria.santos@campus.edu.br", "hashedPassword", UserRole.ALUNO);
        ReflectionTestUtils.setField(aluno, "id", UUID.randomUUID());

        // When
        String token = jwtService.generateToken(aluno);

        // Then
        assertThat(jwtService.extractRole(token)).isEqualTo("ALUNO");
        assertThat(jwtService.extractUserId(token)).isEqualTo(aluno.getId().toString());
    }

    @Test
    void tokenValidation_shouldFailForMalformedToken() {
        // When & Then
        assertThatThrownBy(() -> jwtService.validateToken("not.a.valid.jwt"))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("Invalid JWT token");
    }

    @Test
    void tokenValidation_shouldFailForTokenWithWrongSignature() {
        // Given - A token created with a different service (different secret)
        JwtService differentService = new JwtService(
            "ZGlmZmVyZW50U2VjcmV0S2V5Rm9yVGVzdGluZ1B1cnBvc2VzT25seQ==", // different secret
            3600
        );
        
        User user = new User("Test", "test@example.com", "password", UserRole.PROFESSOR);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        
        String tokenWithWrongSignature = differentService.generateToken(user);

        // When & Then
        assertThatThrownBy(() -> jwtService.validateToken(tokenWithWrongSignature))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("Invalid JWT token");
    }

    @Test
    void configurationValidation_shouldRejectInvalidExpiresIn() {
        // When & Then - Too low
        assertThatThrownBy(() -> new JwtService("dGVzdFNlY3JldA==", 59))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JWT expiresIn must be between 60 and 86400 seconds");
        
        // When & Then - Too high  
        assertThatThrownBy(() -> new JwtService("dGVzdFNlY3JldA==", 86401))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JWT expiresIn must be between 60 and 86400 seconds");
    }
}