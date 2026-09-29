package com.campuslab.shared;

import com.campuslab.auth.AuthService;
import com.campuslab.auth.dto.LoginRequest;
import com.campuslab.laboratory.Laboratory;
import com.campuslab.laboratory.LaboratoryRepository;
import com.campuslab.laboratory.dto.LaboratoryRequest;
import com.campuslab.reservation.Reservation;
import com.campuslab.reservation.ReservationRepository;
import com.campuslab.reservation.dto.ReservationRequest;
import com.campuslab.user.User;
import com.campuslab.user.UserRepository;
import com.campuslab.user.UserRole;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.jqwik.api.*;
import net.jqwik.spring.JqwikSpringSupport;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * Property-Based Test for ErrorHandler — Property 10.
 *
 * Feature: campus-lab, Property 10: Todas as respostas de erro seguem o formato padronizado
 *
 * Validates: Requirements 8.1, 8.3
 *
 * For any operation resulting in an HTTP error (400, 401, 403, 404, 409),
 * the response must contain exactly:
 *   - timestamp: ISO 8601 UTC string
 *   - status: integer equal to the HTTP code
 *   - code: non-empty string
 *   - message: string with at most 512 characters
 *   - path: string corresponding to the endpoint called
 */
@JqwikSpringSupport
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class ErrorHandlerPropertyTest {

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
    @Autowired LaboratoryRepository laboratoryRepository;
    @Autowired ReservationRepository reservationRepository;
    @Autowired BCryptPasswordEncoder passwordEncoder;
    @Autowired AuthService authService;

    // Tokens are initialized once per Spring context lifecycle.
    // @BeforeProperty / @BeforeExample from jqwik can also be used,
    // but since the Spring context is shared across tries within the same
    // property, we rely on @BeforeEach (JUnit 5) which jqwik-spring honours.
    private String professorToken;
    private String alunoToken;

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        laboratoryRepository.deleteAll();
        userRepository.deleteAll();

        userRepository.save(new User("Professor", "prof@error-prop.com",
                passwordEncoder.encode("senha123"), UserRole.PROFESSOR));
        userRepository.save(new User("Aluno", "aluno@error-prop.com",
                passwordEncoder.encode("senha123"), UserRole.ALUNO));

        professorToken = authService.login(
                new LoginRequest("prof@error-prop.com", "senha123")).token();
        alunoToken = authService.login(
                new LoginRequest("aluno@error-prop.com", "senha123")).token();
    }

    // -----------------------------------------------------------------------
    // Provider: one scenario per distinct HTTP error code the ErrorHandler emits
    // -----------------------------------------------------------------------

    /**
     * Each value encodes: (expectedHttpStatus, requestThunk)
     * Using an enum-style string label so jqwik can log which scenario failed.
     */
    record ErrorScenario(String label, int expectedStatus) {}

    @Provide
    Arbitrary<ErrorScenario> errorScenarios() {
        return Arbitraries.of(
                new ErrorScenario("HTTP_400_validation_error", 400),
                new ErrorScenario("HTTP_400_malformed_request", 400),
                new ErrorScenario("HTTP_400_invalid_reservation_period", 400),
                new ErrorScenario("HTTP_401_no_token", 401),
                new ErrorScenario("HTTP_401_bad_credentials", 401),
                new ErrorScenario("HTTP_403_aluno_writes_lab", 403),
                new ErrorScenario("HTTP_404_lab_not_found", 404),
                new ErrorScenario("HTTP_409_duplicate_lab_name", 409),
                new ErrorScenario("HTTP_409_reservation_conflict", 409),
                new ErrorScenario("HTTP_409_lab_has_future_reservations", 409)
        );
    }

    // -----------------------------------------------------------------------
    // Property 10
    // -----------------------------------------------------------------------

    /**
     * Feature: campus-lab, Property 10: Todas as respostas de erro seguem o formato padronizado
     *
     * For every error scenario, the JSON response must contain the five required
     * fields with correct types and constraints.
     */
    @Property(tries = 50)
    void allErrorResponsesFollowStandardFormat(
            @ForAll("errorScenarios") ErrorScenario scenario) throws Exception {

        MvcResult result = executeScenario(scenario);

        String body = result.getResponse().getContentAsString();
        int actualStatus = result.getResponse().getStatus();
        JsonNode json = objectMapper.readTree(body);

        // --- status code matches expected ---
        assertThat(actualStatus)
                .as("Scenario [%s] should return HTTP %d", scenario.label(), scenario.expectedStatus())
                .isEqualTo(scenario.expectedStatus());

        // --- 'timestamp' field: ISO 8601 parseable ---
        assertThat(json.has("timestamp"))
                .as("Response must contain 'timestamp' field [%s]", scenario.label())
                .isTrue();
        String timestampStr = json.get("timestamp").asText();
        assertThat(timestampStr)
                .as("'timestamp' must be a non-empty ISO 8601 string [%s]", scenario.label())
                .isNotBlank();
        // Verify it is parseable as an Instant (ISO 8601 UTC)
        Instant parsedTimestamp = Instant.parse(timestampStr);
        assertThat(parsedTimestamp).isNotNull();

        // --- 'status' field: integer matching HTTP code ---
        assertThat(json.has("status"))
                .as("Response must contain 'status' field [%s]", scenario.label())
                .isTrue();
        assertThat(json.get("status").isInt())
                .as("'status' must be an integer [%s]", scenario.label())
                .isTrue();
        assertThat(json.get("status").asInt())
                .as("'status' must equal HTTP status code [%s]", scenario.label())
                .isEqualTo(scenario.expectedStatus());

        // --- 'code' field: non-empty string, distinct from status number ---
        assertThat(json.has("code"))
                .as("Response must contain 'code' field [%s]", scenario.label())
                .isTrue();
        String code = json.get("code").asText();
        assertThat(code)
                .as("'code' must be non-empty [%s]", scenario.label())
                .isNotBlank();
        assertThat(code)
                .as("'code' must be a string, not a number like the HTTP status [%s]", scenario.label())
                .doesNotMatch("^\\d+$");

        // --- 'message' field: string with at most 512 characters ---
        assertThat(json.has("message"))
                .as("Response must contain 'message' field [%s]", scenario.label())
                .isTrue();
        String message = json.get("message").asText();
        assertThat(message.length())
                .as("'message' must be at most 512 characters [%s]", scenario.label())
                .isLessThanOrEqualTo(512);

        // --- 'path' field: non-empty string ---
        assertThat(json.has("path"))
                .as("Response must contain 'path' field [%s]", scenario.label())
                .isTrue();
        String path = json.get("path").asText();
        assertThat(path)
                .as("'path' must be a non-empty string [%s]", scenario.label())
                .isNotBlank();

        // --- No extra implementation details leaked ---
        assertThat(body).as("Stack trace must not be in response [%s]", scenario.label())
                .doesNotContain("at com.");
        assertThat(body).as("Class names must not be in response [%s]", scenario.label())
                .doesNotContain("Exception");
        assertThat(body).as("SQL details must not be in response [%s]", scenario.label())
                .doesNotContainIgnoringCase("select ");
    }

    // -----------------------------------------------------------------------
    // Scenario execution dispatcher
    // -----------------------------------------------------------------------

    private MvcResult executeScenario(ErrorScenario scenario) throws Exception {
        return switch (scenario.label()) {
            case "HTTP_400_validation_error" -> execute400ValidationError();
            case "HTTP_400_malformed_request" -> execute400MalformedRequest();
            case "HTTP_400_invalid_reservation_period" -> execute400InvalidReservationPeriod();
            case "HTTP_401_no_token" -> execute401NoToken();
            case "HTTP_401_bad_credentials" -> execute401BadCredentials();
            case "HTTP_403_aluno_writes_lab" -> execute403AlunoWritesLab();
            case "HTTP_404_lab_not_found" -> execute404LabNotFound();
            case "HTTP_409_duplicate_lab_name" -> execute409DuplicateLabName();
            case "HTTP_409_reservation_conflict" -> execute409ReservationConflict();
            case "HTTP_409_lab_has_future_reservations" -> execute409LabHasFutureReservations();
            default -> throw new IllegalArgumentException("Unknown scenario: " + scenario.label());
        };
    }

    // -----------------------------------------------------------------------
    // Individual scenario implementations
    // -----------------------------------------------------------------------

    /** HTTP 400 — @Valid constraint violation on LaboratoryRequest */
    private MvcResult execute400ValidationError() throws Exception {
        LaboratoryRequest req = new LaboratoryRequest("", ""); // blank fields → VALIDATION_ERROR
        return mockMvc.perform(post("/api/laboratories")
                        .header("Authorization", "Bearer " + professorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andReturn();
    }

    /** HTTP 400 — Missing request body → MALFORMED_REQUEST */
    private MvcResult execute400MalformedRequest() throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON))
                .andReturn();
    }

    /** HTTP 400 — startAt == endAt → INVALID_RESERVATION_PERIOD */
    private MvcResult execute400InvalidReservationPeriod() throws Exception {
        Laboratory lab = laboratoryRepository.save(new Laboratory("Lab Period", "Bloco P"));
        Instant now = Instant.now().plusSeconds(3600);
        ReservationRequest req = new ReservationRequest(now, now); // startAt == endAt
        return mockMvc.perform(post("/api/laboratories/{labId}/reservations", lab.getId())
                        .header("Authorization", "Bearer " + professorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andReturn();
    }

    /** HTTP 401 — No Authorization header on protected endpoint */
    private MvcResult execute401NoToken() throws Exception {
        return mockMvc.perform(get("/api/laboratories"))
                .andReturn();
    }

    /** HTTP 401 — Wrong credentials at login */
    private MvcResult execute401BadCredentials() throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"prof@error-prop.com\",\"password\":\"wrongpassword\"}"))
                .andReturn();
    }

    /** HTTP 403 — ALUNO tries to create a laboratory → INSUFFICIENT_PERMISSIONS */
    private MvcResult execute403AlunoWritesLab() throws Exception {
        LaboratoryRequest req = new LaboratoryRequest("Lab Aluno", "Bloco A");
        return mockMvc.perform(post("/api/laboratories")
                        .header("Authorization", "Bearer " + alunoToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andReturn();
    }

    /** HTTP 404 — Laboratory not found */
    private MvcResult execute404LabNotFound() throws Exception {
        return mockMvc.perform(get("/api/laboratories/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + alunoToken))
                .andReturn();
    }

    /** HTTP 409 — Duplicate laboratory name */
    private MvcResult execute409DuplicateLabName() throws Exception {
        String uniqueName = "Lab Dup " + UUID.randomUUID();
        laboratoryRepository.save(new Laboratory(uniqueName, "Bloco X"));
        LaboratoryRequest req = new LaboratoryRequest(uniqueName, "Bloco Y");
        return mockMvc.perform(post("/api/laboratories")
                        .header("Authorization", "Bearer " + professorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andReturn();
    }

    /** HTTP 409 — Overlapping reservations */
    private MvcResult execute409ReservationConflict() throws Exception {
        Laboratory lab = laboratoryRepository.save(new Laboratory("Lab Conflict " + UUID.randomUUID(), "Bloco C"));
        User professor = userRepository.findByEmail("prof@error-prop.com").orElseThrow();

        Instant start = Instant.now().plusSeconds(3600);
        Instant end = start.plus(2, ChronoUnit.HOURS);

        // Create first reservation directly via repository
        Reservation existing = new Reservation(lab, professor, start, end);
        reservationRepository.save(existing);

        // Attempt to create a second overlapping reservation via API
        ReservationRequest req = new ReservationRequest(
                start.plusSeconds(1800),
                end.plusSeconds(1800)
        );
        return mockMvc.perform(post("/api/laboratories/{labId}/reservations", lab.getId())
                        .header("Authorization", "Bearer " + professorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andReturn();
    }

    /** HTTP 409 — Delete lab that has future reservations */
    private MvcResult execute409LabHasFutureReservations() throws Exception {
        Laboratory lab = laboratoryRepository.save(new Laboratory("Lab Future " + UUID.randomUUID(), "Bloco F"));
        User professor = userRepository.findByEmail("prof@error-prop.com").orElseThrow();

        Instant start = Instant.now().plusSeconds(7200);
        Instant end = start.plus(1, ChronoUnit.HOURS);
        reservationRepository.save(new Reservation(lab, professor, start, end));

        return mockMvc.perform(delete("/api/laboratories/{id}", lab.getId())
                        .header("Authorization", "Bearer " + professorToken))
                .andReturn();
    }
}
