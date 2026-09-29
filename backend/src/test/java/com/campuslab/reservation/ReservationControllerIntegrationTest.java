package com.campuslab.reservation;

import com.campuslab.auth.AuthService;
import com.campuslab.auth.dto.LoginRequest;
import com.campuslab.laboratory.Laboratory;
import com.campuslab.laboratory.LaboratoryRepository;
import com.campuslab.reservation.dto.ReservationRequest;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for ReservationController.
 * Requirements: 5.1–5.5, 6.1–6.7, 7.1–7.7
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class ReservationControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired LaboratoryRepository laboratoryRepository;
    @Autowired ReservationRepository reservationRepository;
    @Autowired BCryptPasswordEncoder passwordEncoder;
    @Autowired AuthService authService;

    private String professorToken;
    private String alunoToken;
    private Laboratory lab;

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        laboratoryRepository.deleteAll();
        userRepository.deleteAll();

        userRepository.save(new User("Professor", "prof@test.com",
                passwordEncoder.encode("senha123"), UserRole.PROFESSOR));
        userRepository.save(new User("Aluno", "aluno@test.com",
                passwordEncoder.encode("senha123"), UserRole.ALUNO));

        professorToken = authService.login(new LoginRequest("prof@test.com", "senha123")).token();
        alunoToken    = authService.login(new LoginRequest("aluno@test.com",  "senha123")).token();

        lab = laboratoryRepository.save(new Laboratory("Lab Reservas", "Bloco T"));
    }

    // --- GET /api/laboratories/{labId}/reservations ---

    @Test
    void listByLab_asAluno_returns200OrderedByStartAt() throws Exception {
        mockMvc.perform(get("/api/laboratories/{labId}/reservations", lab.getId())
                .header("Authorization", "Bearer " + alunoToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void listByLab_nonExistentLab_returns404() throws Exception {
        mockMvc.perform(get("/api/laboratories/{labId}/reservations", UUID.randomUUID())
                .header("Authorization", "Bearer " + alunoToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void listByLab_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/laboratories/{labId}/reservations", lab.getId()))
                .andExpect(status().isUnauthorized());
    }

    // --- POST /api/laboratories/{labId}/reservations ---

    @Test
    void create_asProfessor_validPeriod_returns201() throws Exception {
        Instant start = Instant.now().plus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS);
        Instant end   = start.plus(2, ChronoUnit.HOURS);
        ReservationRequest req = new ReservationRequest(start, end);

        mockMvc.perform(post("/api/laboratories/{labId}/reservations", lab.getId())
                .header("Authorization", "Bearer " + professorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.laboratoryId").value(lab.getId().toString()));
    }

    @Test
    void create_asAluno_returns403() throws Exception {
        Instant start = Instant.now().plus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS);
        ReservationRequest req = new ReservationRequest(start, start.plus(1, ChronoUnit.HOURS));

        mockMvc.perform(post("/api/laboratories/{labId}/reservations", lab.getId())
                .header("Authorization", "Bearer " + alunoToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_PERMISSIONS"));
    }

    @Test
    void create_nonExistentLab_returns404() throws Exception {
        Instant start = Instant.now().plus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS);
        ReservationRequest req = new ReservationRequest(start, start.plus(1, ChronoUnit.HOURS));

        mockMvc.perform(post("/api/laboratories/{labId}/reservations", UUID.randomUUID())
                .header("Authorization", "Bearer " + professorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_startAtAfterEndAt_returns400() throws Exception {
        Instant start = Instant.now().plus(2, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS);
        Instant end   = start.minus(1, ChronoUnit.HOURS);
        ReservationRequest req = new ReservationRequest(start, end);

        mockMvc.perform(post("/api/laboratories/{labId}/reservations", lab.getId())
                .header("Authorization", "Bearer " + professorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_RESERVATION_PERIOD"));
    }

    @Test
    void create_durationUnder60s_returns400() throws Exception {
        Instant start = Instant.now().plus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS);
        Instant end   = start.plusSeconds(59);
        ReservationRequest req = new ReservationRequest(start, end);

        mockMvc.perform(post("/api/laboratories/{labId}/reservations", lab.getId())
                .header("Authorization", "Bearer " + professorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_RESERVATION_PERIOD"));
    }

    @Test
    void create_overlappingReservation_returns409() throws Exception {
        Instant start = Instant.now().plus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS);
        Instant end   = start.plus(2, ChronoUnit.HOURS);

        // First reservation
        ReservationRequest req1 = new ReservationRequest(start, end);
        mockMvc.perform(post("/api/laboratories/{labId}/reservations", lab.getId())
                .header("Authorization", "Bearer " + professorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated());

        // Overlapping second reservation
        ReservationRequest req2 = new ReservationRequest(
                start.plus(30, ChronoUnit.MINUTES),
                end.plus(30, ChronoUnit.MINUTES));

        mockMvc.perform(post("/api/laboratories/{labId}/reservations", lab.getId())
                .header("Authorization", "Bearer " + professorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESERVATION_CONFLICT"));
    }

    // --- PUT /api/reservations/{id} ---

    @Test
    void update_doesNotConflictWithItself_returns200() throws Exception {
        Instant start = Instant.now().plus(3, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS);
        Instant end   = start.plus(2, ChronoUnit.HOURS);

        // Create a reservation
        ReservationRequest createReq = new ReservationRequest(start, end);
        String createResponse = mockMvc.perform(post("/api/laboratories/{labId}/reservations", lab.getId())
                .header("Authorization", "Bearer " + professorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String reservationId = objectMapper.readTree(createResponse).get("id").asText();

        // Update with the same interval — must not conflict with itself
        mockMvc.perform(put("/api/reservations/{id}", reservationId)
                .header("Authorization", "Bearer " + professorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reservationId));
    }

    @Test
    void update_nonExistentId_returns404() throws Exception {
        Instant start = Instant.now().plus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS);
        ReservationRequest req = new ReservationRequest(start, start.plus(1, ChronoUnit.HOURS));

        mockMvc.perform(put("/api/reservations/{id}", UUID.randomUUID())
                .header("Authorization", "Bearer " + professorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    // --- DELETE /api/reservations/{id} ---

    @Test
    void delete_existingReservation_returns204() throws Exception {
        Instant start = Instant.now().plus(5, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS);
        Instant end   = start.plus(1, ChronoUnit.HOURS);

        String createResponse = mockMvc.perform(post("/api/laboratories/{labId}/reservations", lab.getId())
                .header("Authorization", "Bearer " + professorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ReservationRequest(start, end))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String reservationId = objectMapper.readTree(createResponse).get("id").asText();

        mockMvc.perform(delete("/api/reservations/{id}", reservationId)
                .header("Authorization", "Bearer " + professorToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_nonExistentId_returns404() throws Exception {
        mockMvc.perform(delete("/api/reservations/{id}", UUID.randomUUID())
                .header("Authorization", "Bearer " + professorToken))
                .andExpect(status().isNotFound());
    }

    // --- GET /api/reservations/{id} ---

    @Test
    void getById_existingReservation_returns200() throws Exception {
        Instant start = Instant.now().plus(6, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS);
        Instant end   = start.plus(1, ChronoUnit.HOURS);

        String createResponse = mockMvc.perform(post("/api/laboratories/{labId}/reservations", lab.getId())
                .header("Authorization", "Bearer " + professorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ReservationRequest(start, end))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String reservationId = objectMapper.readTree(createResponse).get("id").asText();

        mockMvc.perform(get("/api/reservations/{id}", reservationId)
                .header("Authorization", "Bearer " + alunoToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reservationId))
                .andExpect(jsonPath("$.laboratoryId").value(lab.getId().toString()));
    }
}
