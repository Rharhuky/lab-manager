package com.campuslab.security;

import com.campuslab.shared.security.JwtService;
import com.campuslab.user.User;
import com.campuslab.user.UserRole;
import io.jsonwebtoken.Claims;
import net.jqwik.api.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-Based Tests for JwtService.
 *
 * Feature: campus-lab, Property 2: JWT gerado contém userId e role corretos
 *
 * Validates: Requirements 1.5
 */
class JwtServicePropertyTest {

    /**
     * Base64-encoded 32-byte key used across all property runs.
     * Value: "testSecretKeyForJwtServiceTests32BytesKey" encoded in base64.
     */
    private static final String TEST_SECRET =
            "dGVzdFNlY3JldEtleUZvckp3dFNlcnZpY2VUZXN0czMyQnl0ZXNLZXk=";

    // ---------------------------------------------------------------------------
    // Property 2: Para qualquer usuário válido (qualquer role), o JWT gerado deve
    //   - conter o userId correto no campo `sub`
    //   - conter a role correta no campo `role`
    //   - ter `exp` igual a `iat + expiresIn`
    // ---------------------------------------------------------------------------

    @Property(tries = 100)
    // Feature: campus-lab, Property 2: JWT gerado contém userId e role corretos
    void jwtTokenContainsCorrectUserIdAndRole(
            @ForAll("validUsers") User user,
            @ForAll("validExpiresIn") long expiresIn
    ) {
        // Arrange
        JwtService jwtService = new JwtService(TEST_SECRET, expiresIn);

        // Act
        Instant before = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        String token = jwtService.generateToken(user);
        Instant after = Instant.now().plusSeconds(1);

        // Assert
        Claims claims = jwtService.validateToken(token);

        // sub must equal userId
        assertThat(claims.getSubject())
                .as("JWT 'sub' must equal the user's UUID")
                .isEqualTo(user.getId().toString());

        // role claim must equal role name
        assertThat(claims.get("role", String.class))
                .as("JWT 'role' claim must equal the user's role name")
                .isEqualTo(user.getRole().name());

        // iat must be present and roughly now
        assertThat(claims.getIssuedAt())
                .as("JWT 'iat' must be present")
                .isNotNull();
        assertThat(claims.getIssuedAt().toInstant())
                .as("JWT 'iat' must be within the generation window")
                .isBetween(before.minusSeconds(1), after);

        // exp must equal iat + expiresIn
        Instant expectedExp = claims.getIssuedAt().toInstant().plusSeconds(expiresIn);
        assertThat(claims.getExpiration().toInstant())
                .as("JWT 'exp' must equal 'iat' + expiresIn (%d s)", expiresIn)
                .isBetween(expectedExp.minusSeconds(1), expectedExp.plusSeconds(1));
    }

    @Property(tries = 100)
    // Feature: campus-lab, Property 2: extractUserId e extractRole retornam valores corretos
    void extractMethodsReturnCorrectValues(
            @ForAll("validUsers") User user
    ) {
        JwtService jwtService = new JwtService(TEST_SECRET, 3600);

        String token = jwtService.generateToken(user);

        assertThat(jwtService.extractUserId(token))
                .as("extractUserId must return the user's UUID string")
                .isEqualTo(user.getId().toString());

        assertThat(jwtService.extractRole(token))
                .as("extractRole must return the user's role name")
                .isEqualTo(user.getRole().name());
    }

    // ---------------------------------------------------------------------------
    // Arbitraries (generators)
    // ---------------------------------------------------------------------------

    /**
     * Generates valid User instances with a random UUID id and one of the two
     * application roles (ALUNO or PROFESSOR).
     */
    @Provide
    Arbitrary<User> validUsers() {
        Arbitrary<UserRole> roles = Arbitraries.of(UserRole.ALUNO, UserRole.PROFESSOR);
        Arbitrary<UUID> uuids = Arbitraries.create(UUID::randomUUID);
        Arbitrary<String> names = Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(1)
                .ofMaxLength(50);
        Arbitrary<String> emails = names.map(n -> n + "@campus.edu.br");

        return Combinators.combine(uuids, names, emails, roles)
                .as((id, name, email, role) -> {
                    User u = new User(name, email, "hashed_password", role);
                    ReflectionTestUtils.setField(u, "id", id);
                    return u;
                });
    }

    /**
     * Generates valid expiresIn values in the allowed range [60, 86400].
     */
    @Provide
    Arbitrary<Long> validExpiresIn() {
        return Arbitraries.longs().between(60L, 86400L);
    }
}
