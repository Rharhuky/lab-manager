package com.campuslab.auth;

import com.campuslab.auth.dto.LoginRequest;
import com.campuslab.user.User;
import com.campuslab.user.UserRepository;
import com.campuslab.user.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for AuthController.
 * Uses Testcontainers for a real PostgreSQL database.
 * Requirements: 1.1, 1.2, 1.3
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired BCryptPasswordEncoder passwordEncoder;

    private static final String EMAIL = "professor@test.com";
    private static final String PASSWORD = "senha123";

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        User user = new User("Professor Teste", EMAIL, passwordEncoder.encode(PASSWORD), UserRole.PROFESSOR);
        userRepository.save(user);
    }

    // --- Success ---

    @Test
    void login_withValidCredentials_returns200WithToken() throws Exception {
        LoginRequest request = new LoginRequest(EMAIL, PASSWORD);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").isNumber())
                .andExpect(jsonPath("$.user.email").value(EMAIL))
                .andExpect(jsonPath("$.user.role").value("PROFESSOR"));
    }

    // --- Invalid credentials ---

    @Test
    void login_withWrongPassword_returns401() throws Exception {
        LoginRequest request = new LoginRequest(EMAIL, "wrongPassword");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").isString())
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.path").value("/api/auth/login"));
    }

    @Test
    void login_withNonExistentEmail_returns401() throws Exception {
        LoginRequest request = new LoginRequest("nobody@test.com", PASSWORD);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    // --- Validation errors (400) ---

    @Test
    void login_withEmptyEmail_returns400() throws Exception {
        LoginRequest request = new LoginRequest("", PASSWORD);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void login_withInvalidEmailFormat_returns400() throws Exception {
        LoginRequest request = new LoginRequest("not-an-email", PASSWORD);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void login_withEmptyPassword_returns400() throws Exception {
        LoginRequest request = new LoginRequest(EMAIL, "");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void login_withMissingBody_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void login_error401_doesNotRevealWhichFieldIsWrong() throws Exception {
        // Wrong email and wrong password should both return the same generic 401
        LoginRequest wrongEmail = new LoginRequest("wrong@test.com", PASSWORD);
        LoginRequest wrongPass = new LoginRequest(EMAIL, "wrong");

        String responseEmail = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(wrongEmail)))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        String responsePass = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(wrongPass)))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        // Both should have the same code (no field discrimination)
        var responseEmailJson = objectMapper.readTree(responseEmail);
        var responsePassJson = objectMapper.readTree(responsePass);
        org.assertj.core.api.Assertions.assertThat(responseEmailJson.get("code").asText())
                .isEqualTo(responsePassJson.get("code").asText());
    }
}
