package com.campuslab.auth;

import net.jqwik.api.*;
import net.jqwik.api.constraints.NotBlank;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for AuthService password encoding.
 * Tests are pure unit tests — no Spring context required.
 *
 * Feature: campus-lab
 * Property 1: BCrypt hash é verificável para qualquer senha
 */
class AuthServicePropertyTest {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * Property 1: BCrypt hash é verificável para qualquer senha
     *
     * For any non-blank password, the BCrypt hash produced by AuthService.encodePassword
     * must:
     *   (a) be verifiable with BCryptPasswordEncoder.matches — i.e., the hash represents the original password
     *   (b) not equal the plain-text password — i.e., it is actually hashed
     *
     * Validates: Requirements 1.4
     */
    @Property(tries = 100)
    // Feature: campus-lab, Property 1: BCrypt hash é verificável para qualquer senha
    void bcryptHashIsAlwaysVerifiable(@ForAll @NotBlank String password) {
        AuthService authService = buildAuthService();

        String hash = authService.encodePassword(password);

        // (a) hash must match the original plain-text password
        assertThat(passwordEncoder.matches(password, hash))
                .as("BCrypt.matches(%s, hash) should be true", password)
                .isTrue();

        // (b) the stored hash must never equal the plain-text password
        assertThat(hash)
                .as("The BCrypt hash must not equal the plain-text password")
                .isNotEqualTo(password);
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    /**
     * Builds a minimal AuthService wired only with a BCryptPasswordEncoder.
     * The property under test (encodePassword) does not depend on the other
     * collaborators (UserRepository, JwtService, expiresIn).
     */
    private AuthService buildAuthService() {
        return new AuthService(
                /* userRepository */ null,
                passwordEncoder,
                /* jwtService    */ null,
                /* expiresIn     */ 3600L
        );
    }
}
