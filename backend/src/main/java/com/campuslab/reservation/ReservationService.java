package com.campuslab.reservation;

import com.campuslab.laboratory.Laboratory;
import com.campuslab.laboratory.LaboratoryRepository;
import com.campuslab.reservation.dto.ReservationRequest;
import com.campuslab.reservation.dto.ReservationResponse;
import com.campuslab.shared.exception.exceptions.InvalidReservationPeriodException;
import com.campuslab.shared.exception.exceptions.ReservationConflictException;
import com.campuslab.shared.exception.exceptions.ResourceNotFoundException;
import com.campuslab.user.User;
import com.campuslab.user.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Business logic for reservation CRUD operations.
 * Requirements: 5.1–5.5, 6.1–6.7, 7.1–7.7
 */
@Service
public class ReservationService {

    // Nil UUID used for create scenarios where no reservation should be excluded
    private static final UUID NIL_UUID = new UUID(0L, 0L);

    private final ReservationRepository reservationRepository;
    private final LaboratoryRepository laboratoryRepository;
    private final UserRepository userRepository;

    public ReservationService(ReservationRepository reservationRepository,
                               LaboratoryRepository laboratoryRepository,
                               UserRepository userRepository) {
        this.reservationRepository = reservationRepository;
        this.laboratoryRepository = laboratoryRepository;
        this.userRepository = userRepository;
    }

    /**
     * Lists all reservations for a laboratory, ordered by startAt ascending.
     *
     * @throws ResourceNotFoundException if laboratory not found
     */
    @Transactional(readOnly = true)
    public List<ReservationResponse> findByLaboratory(UUID laboratoryId) {
        if (!laboratoryRepository.existsById(laboratoryId)) {
            throw new ResourceNotFoundException("Laboratório não encontrado com id: " + laboratoryId);
        }
        return reservationRepository.findByLaboratoryIdOrderByStartAtAsc(laboratoryId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Returns a single reservation by id.
     *
     * @throws ResourceNotFoundException if not found
     */
    @Transactional(readOnly = true)
    public ReservationResponse findById(UUID id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva não encontrada com id: " + id));
        return toResponse(reservation);
    }

    /**
     * Creates a new reservation for a laboratory.
     *
     * @throws ResourceNotFoundException       if laboratory not found
     * @throws InvalidReservationPeriodException if period is invalid
     * @throws ReservationConflictException    if there is a time conflict
     */
    @Transactional
    public ReservationResponse create(UUID laboratoryId, ReservationRequest request) {
        Laboratory laboratory = laboratoryRepository.findById(laboratoryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Laboratório não encontrado com id: " + laboratoryId));

        validatePeriod(request.startAt(), request.endAt());
        checkConflict(laboratoryId, NIL_UUID, request.startAt(), request.endAt());

        User user = getCurrentUser();
        Reservation reservation = new Reservation(laboratory, user, request.startAt(), request.endAt());
        reservation = reservationRepository.save(reservation);

        return toResponse(reservation);
    }

    /**
     * Updates an existing reservation (excludes itself from conflict check).
     *
     * @throws ResourceNotFoundException       if reservation not found
     * @throws InvalidReservationPeriodException if period is invalid
     * @throws ReservationConflictException    if there is a time conflict with another reservation
     */
    @Transactional
    public ReservationResponse update(UUID id, ReservationRequest request) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva não encontrada com id: " + id));

        validatePeriod(request.startAt(), request.endAt());
        checkConflict(reservation.getLaboratory().getId(), id, request.startAt(), request.endAt());

        reservation.setStartAt(request.startAt());
        reservation.setEndAt(request.endAt());
        reservation = reservationRepository.save(reservation);

        return toResponse(reservation);
    }

    /**
     * Deletes a reservation.
     *
     * @throws ResourceNotFoundException if not found
     */
    @Transactional
    public void delete(UUID id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva não encontrada com id: " + id));
        reservationRepository.delete(reservation);
    }

    // --- private helpers ---

    /**
     * Validates that startAt < endAt and duration >= 60 seconds.
     */
    private void validatePeriod(Instant startAt, Instant endAt) {
        if (!startAt.isBefore(endAt)) {
            throw new InvalidReservationPeriodException(
                    "O horário de início deve ser anterior ao horário de término");
        }
        if (Duration.between(startAt, endAt).toSeconds() < 60) {
            throw new InvalidReservationPeriodException(
                    "A duração mínima de uma reserva é de 1 minuto");
        }
    }

    /**
     * Checks for overlapping active reservations.
     * Pass excludeId = NIL_UUID for creates, or the reservation's own id for updates.
     */
    private void checkConflict(UUID laboratoryId, UUID excludeId, Instant startAt, Instant endAt) {
        if (reservationRepository.existsConflict(laboratoryId, excludeId, startAt, endAt, Instant.now())) {
            throw new ReservationConflictException(
                    "Existe conflito de horário com outra reserva ativa no mesmo laboratório");
        }
    }

    private ReservationResponse toResponse(Reservation r) {
        return new ReservationResponse(
                r.getId(),
                r.getLaboratory().getId(),
                r.getUser().getId(),
                r.getStartAt(),
                r.getEndAt(),
                r.getCreatedAt()
        );
    }

    /**
     * Retrieves the authenticated user from the SecurityContext.
     * The principal is the userId (UUID string) set by SecurityFilter.
     */
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UUID userId = UUID.fromString((String) auth.getPrincipal());
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário autenticado não encontrado"));
    }
}
