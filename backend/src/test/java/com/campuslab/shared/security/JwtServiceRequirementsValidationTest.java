package com.campuslab.shared.security;

import com.campuslab.user.User;
import com.campuslab.user.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Validation tests to ensure JwtService meets the specific requirements 1.5 and 2.3
 * from the CampusLab specification.
 */
class JwtServiceRequirementsValidationTest {

    @Test
    @DisplayName("Requirement 1.5: JWT_Token SHALL contain user identifier and role (ALUNO/PROFESSOR), with expiresIn between 60-86400 seconds")
    void requirement1_5_jwtTokenStructureAndExpiration() {
        // Test with valid expiresIn configurations
        int[] validExpiresIn = {60, 3600, 86400}; // min, typical, max
        UserRole[] roles = {UserRole.ALUNO, UserRole.PROFESSOR};

        for (int expiresIn : validExpiresIn) {
            for (UserRole role : roles) {
                // Given
                String testSecret = "dGVzdFNlY3JldEtleUZvckp3dFNlcnZpY2VUZXN0czMyQnl0ZXNLZXk=";
                JwtService jwtService = new JwtService(testSecret, expiresIn);
                
                User user = new User("Test User", "test@example.com", "password", role);
                UUID userId = UUID.randomUUID();
                ReflectionTestUtils.setField(user, "id", userId);

                // When
                Instant beforeGeneration = Instant.now().truncatedTo(ChronoUnit.SECONDS);
                String token = jwtService.generateToken(user);
                Instant afterGeneration = Instant.now().truncatedTo(ChronoUnit.SECONDS);

                // Then - Validate token contains required fields
                Claims claims = jwtService.validateToken(token);
                
                // User identifier must be present in 'sub' claim
                assertThat(claims.getSubject())
                    .as("JWT must contain user identifier in 'sub' claim")
                    .isEqualTo(userId.toString());
                
                // Role must be present in 'role' claim
                assertThat(claims.get("role", String.class))
                    .as("JWT must contain user role in 'role' claim")
                    .isEqualTo(role.name());
                
                // Issued at must be present
                assertThat(claims.getIssuedAt())
                    .as("JWT must contain 'iat' claim")
                    .isNotNull()
                    .isBetween(
                        Date.from(beforeGeneration.minusSeconds(1)),
                        Date.from(afterGeneration.plusSeconds(1))
                    );
                
                // Expiration must be iat + expiresIn
                Instant expectedExpiration = claims.getIssuedAt().toInstant().plusSeconds(expiresIn);
                assertThat(claims.getExpiration().toInstant())
                    .as("JWT expiration must be iat + expiresIn (%d seconds)", expiresIn)
                    .isBetween(
                        expectedExpiration.minusSeconds(1),
                        expectedExpiration.plusSeconds(1)
                    );
                
                // Verify extraction methods work correctly
                assertThat(jwtService.extractUserId(token))
                    .as("extractUserId must return correct user ID")
                    .isEqualTo(userId.toString());
                
                assertThat(jwtService.extractRole(token))
                    .as("extractRole must return correct role")
                    .isEqualTo(role.name());
            }
        }
    }

    @Test
    @DisplayName("Requirement 1.5: JWT_Token configuration SHALL validate expiresIn between 60 and 86400 seconds")
    void requirement1_5_expiresInValidation() {
        String testSecret = "dGVzdFNlY3JldEtleUZvckp3dFNlcnZpY2VUZXN0czMyQnl0ZXNLZXk=";

        // Test invalid expiresIn values (below minimum)
        assertThatThrownBy(() -> new JwtService(testSecret, 59))
            .as("expiresIn below 60 seconds should be rejected")
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("JWT expiresIn must be between 60 and 86400 seconds, but was: 59");

        // Test invalid expiresIn values (above maximum)
        assertThatThrownBy(() -> new JwtService(testSecret, 86401))
            .as("expiresIn above 86400 seconds should be rejected")
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("JWT expiresIn must be between 60 and 86400 seconds, but was: 86401");

        // Test boundary values (should be accepted)
        assertThatCode(() -> new JwtService(testSecret, 60))
            .as("expiresIn of exactly 60 seconds should be accepted")
            .doesNotThrowAnyException();

        assertThatCode(() -> new JwtService(testSecret, 86400))
            .as("expiresIn of exactly 86400 seconds should be accepted")
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Requirement 2.3: JWT_Token with invalid signature or malformed structure SHALL cause JwtException")
    void requirement2_3_invalidTokenValidation() {
        // Given
        String testSecret = "dGVzdFNlY3JldEtleUZvckp3dFNlcnZpY2VUZXN0czMyQnl0ZXNLZXk=";
        JwtService jwtService = new JwtService(testSecret, 3600);

        // Test cases for invalid tokens
        String[] invalidTokens = {
            // Completely malformed tokens
            "not.a.jwt.token",
            "invalid_token_format",
            "header.payload", // Missing signature
            "too.many.parts.in.token.here",
            
            // Empty/null tokens
            "",
            "   ", // whitespace only
            
            // Tokens with wrong structure
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.invalid_payload.signature",
            "header.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.invalid_signature"
        };

        for (String invalidToken : invalidTokens) {
            if (invalidToken == null || invalidToken.trim().isEmpty()) {
                // Special handling for null/empty tokens
                assertThatThrownBy(() -> jwtService.validateToken(invalidToken))
                    .as("Null/empty token '%s' should throw JwtException", invalidToken)
                    .isInstanceOf(JwtException.class)
                    .hasMessageContaining("Token cannot be null or empty");
            } else {
                // Other malformed tokens should throw generic invalid token message
                assertThatThrownBy(() -> jwtService.validateToken(invalidToken))
                    .as("Invalid token '%s' should throw JwtException", invalidToken)
                    .isInstanceOf(JwtException.class)
                    .hasMessageContaining("Invalid JWT token");
            }
        }

        // Test null token specifically
        assertThatThrownBy(() -> jwtService.validateToken(null))
            .as("Null token should throw JwtException")
            .isInstanceOf(JwtException.class)
            .hasMessageContaining("Token cannot be null or empty");
    }

    @Test
    @DisplayName("Requirement 2.3: JWT_Token with invalid signature (different secret) SHALL cause JwtException")
    void requirement2_3_wrongSignatureValidation() {
        // Given - Two services with different secrets
        String secret1 = "dGVzdFNlY3JldEtleUZvckp3dFNlcnZpY2VUZXN0czMyQnl0ZXNLZXk=";
        String secret2 = "ZGlmZmVyZW50U2VjcmV0S2V5Rm9yVGVzdGluZ1B1cnBvc2VzT25seQ==";
        
        JwtService service1 = new JwtService(secret1, 3600);
        JwtService service2 = new JwtService(secret2, 3600);
        
        User user = new User("Test User", "test@example.com", "password", UserRole.PROFESSOR);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());

        // When - Generate token with service1, try to validate with service2
        String tokenFromService1 = service1.generateToken(user);

        // Then - service2 should reject the token due to different signature
        assertThatThrownBy(() -> service2.validateToken(tokenFromService1))
            .as("Token with different signature should be rejected")
            .isInstanceOf(JwtException.class)
            .hasMessageContaining("Invalid JWT token");

        // Sanity check - service1 should accept its own token
        assertThatCode(() -> service1.validateToken(tokenFromService1))
            .as("Service should accept its own token")
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Requirement 2.3: Extraction methods SHALL fail for invalid tokens")
    void requirement2_3_extractionMethodsFailForInvalidTokens() {
        // Given
        String testSecret = "dGVzdFNlY3JldEtleUZvckp3dFNlcnZpY2VUZXN0czMyQnl0ZXNLZXk=";
        JwtService jwtService = new JwtService(testSecret, 3600);

        String invalidToken = "invalid.jwt.token";

        // When & Then - Both extraction methods should fail for invalid token
        assertThatThrownBy(() -> jwtService.extractUserId(invalidToken))
            .as("extractUserId should fail for invalid token")
            .isInstanceOf(JwtException.class)
            .hasMessageContaining("Invalid JWT token");

        assertThatThrownBy(() -> jwtService.extractRole(invalidToken))
            .as("extractRole should fail for invalid token")
            .isInstanceOf(JwtException.class)
            .hasMessageContaining("Invalid JWT token");
    }
}