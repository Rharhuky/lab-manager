package com.campuslab.security;

import com.campuslab.laboratory.LaboratoryController;
import com.campuslab.laboratory.LaboratoryService;
import com.campuslab.shared.config.SecurityConfig;
import com.campuslab.shared.exception.ErrorHandler;
import com.campuslab.shared.security.JwtService;
import com.campuslab.shared.security.SecurityFilter;
import net.jqwik.api.*;
import net.jqwik.spring.JqwikSpringSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Property-based tests for SecurityFilter.
 *
 * Uses @WebMvcTest to load only the web layer (no database required).
 * JwtService, SecurityFilter, SecurityConfig and ErrorHandler are imported
 * explicitly so Spring Security is fully wired without a datasource.
 *
 * Feature: campus-lab
 * Property 3: Tokens inválidos sempre retornam 401
 *
 * Validates: Requirements 2.3
 */
@JqwikSpringSupport
@WebMvcTest(controllers = LaboratoryController.class)
@Import({JwtService.class, SecurityFilter.class, SecurityConfig.class, ErrorHandler.class})
@TestPropertySource(properties = {
    "jwt.secret=dGVzdFNlY3JldEtleUZvckp3dFNlcnZpY2VUZXN0czMyQnl0ZXNLZXk=",
    "jwt.expiresIn=3600"
})
class SecurityFilterPropertyTest {

    @MockBean
    private LaboratoryService laboratoryService;

    // ---------------------------------------------------------------------------
    // Properties
    // ---------------------------------------------------------------------------

    /**
     * Property 3: Tokens inválidos sempre retornam 401
     *
     * For any printable ASCII string (no control characters, no whitespace) that is
     * NOT a valid JWT signed with the configured secret key, sending it as a Bearer
     * token to a protected endpoint must always return HTTP 401 with the standardized
     * error format.
     *
     * The generator restricts to printable ASCII chars (0x21-0x7E) so that the string
     * is always a well-formed HTTP header value. Every such string is an invalid JWT.
     *
     * // Feature: campus-lab, Property 3: Tokens inválidos sempre retornam 401
     * Validates: Requirements 2.3
     */
    @Property(tries = 100)
    // Feature: campus-lab, Property 3: Tokens inválidos sempre retornam 401
    void invalidTokensAlwaysReturn401(@ForAll("validHeaderStrings") String invalidToken,
                                      @Autowired MockMvc mvc) throws Exception {
        mvc.perform(get("/api/laboratories")
                .header("Authorization", "Bearer " + invalidToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").isString())
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.path").value("/api/laboratories"));
    }

    /**
     * Property 3 (variant): JWT-shaped tokens with wrong signature always return 401
     *
     * Strings that follow the "header.payload.signature" JWT structure but whose
     * content and signature do not correspond to a valid signed token from the
     * configured secret must also be rejected with HTTP 401.
     *
     * // Feature: campus-lab, Property 3: Tokens inválidos sempre retornam 401
     * Validates: Requirements 2.3
     */
    @Property(tries = 100)
    // Feature: campus-lab, Property 3: Tokens inválidos sempre retornam 401
    void malformedJwtStructuresAlwaysReturn401(@ForAll("malformedJwtTokens") String malformedToken,
                                               @Autowired MockMvc mvc) throws Exception {
        mvc.perform(get("/api/laboratories")
                .header("Authorization", "Bearer " + malformedToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    /**
     * Property 3 (variant): Missing Authorization header always returns 401
     *
     * Accessing a protected endpoint with no Authorization header must always
     * return HTTP 401 with the standardized error format.
     *
     * // Feature: campus-lab, Property 3: Tokens inválidos sempre retornam 401
     * Validates: Requirements 2.1, 2.3
     */
    @Property(tries = 10)
    // Feature: campus-lab, Property 3: Tokens inválidos sempre retornam 401
    void missingAuthorizationHeaderAlwaysReturns401(@Autowired MockMvc mvc) throws Exception {
        mvc.perform(get("/api/laboratories"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").isString());
    }

    // ---------------------------------------------------------------------------
    // Arbitraries (generators)
    // ---------------------------------------------------------------------------

    /**
     * Generates strings using only printable, non-whitespace ASCII characters (0x21-0x7E).
     * This guarantees that every generated value:
     *   (a) is a valid HTTP header token (no control chars / newlines)
     *   (b) is not a valid JWT signed by the test secret, so the filter must reject it
     */
    @Provide
    Arbitrary<String> validHeaderStrings() {
        return Arbitraries.strings()
                .withCharRange('\u0021', '\u007E') // printable ASCII, no space, no DEL
                .ofMinLength(1)
                .ofMaxLength(256);
    }

    /**
     * Generates strings resembling JWT structure (header.payload.signature) but with
     * arbitrary alphanumeric segments — the signature will never match the configured
     * secret, so every generated value is an invalid JWT.
     */
    @Provide
    Arbitrary<String> malformedJwtTokens() {
        Arbitrary<String> segment = Arbitraries.strings()
                .alpha()
                .numeric()
                .withChars('-', '_')
                .ofMinLength(4)
                .ofMaxLength(64);

        return Combinators.combine(segment, segment, segment)
                .as((header, payload, signature) -> header + "." + payload + "." + signature);
    }
}
