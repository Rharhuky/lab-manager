package com.campuslab.laboratory;

import com.campuslab.laboratory.dto.LaboratoryDetailResponse;
import com.campuslab.laboratory.dto.LaboratoryRequest;
import com.campuslab.laboratory.dto.LaboratoryResponse;
import com.campuslab.reservation.ReservationRepository;
import com.campuslab.reservation.dto.ReservationResponse;
import com.campuslab.shared.exception.exceptions.DuplicateNameException;
import com.campuslab.shared.exception.exceptions.LaboratoryHasFutureReservationsException;
import com.campuslab.shared.exception.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Business logic for laboratory CRUD operations.
 * Requirements: 3.1–3.6, 4.1–4.7
 */
@Service
public class LaboratoryService {

    private final LaboratoryRepository laboratoryRepository;
    private final ReservationRepository reservationRepository;

    public LaboratoryService(LaboratoryRepository laboratoryRepository,
                              ReservationRepository reservationRepository) {
        this.laboratoryRepository = laboratoryRepository;
        this.reservationRepository = reservationRepository;
    }

    /**
     * Lists all laboratories with their reservation status.
     * The 'reserved' field is true when there is at least one future reservation.
     */
    @Transactional(readOnly = true)
    public List<LaboratoryResponse> findAll() {
        Instant now = Instant.now();
        return laboratoryRepository.findAll().stream()
                .map(lab -> toResponse(lab, now))
                .toList();
    }

    /**
     * Returns detailed laboratory data including all reservations.
     *
     * @throws ResourceNotFoundException if laboratory not found
     */
    @Transactional(readOnly = true)
    public LaboratoryDetailResponse findById(UUID id) {
        Laboratory lab = laboratoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Laboratório não encontrado com id: " + id));

        Instant now = Instant.now();
        boolean reserved = reservationRepository.existsFutureReservation(id, now);

        List<ReservationResponse> reservations = reservationRepository
                .findByLaboratoryIdOrderByStartAtAsc(id)
                .stream()
                .map(r -> new ReservationResponse(
                        r.getId(),
                        r.getLaboratory().getId(),
                        r.getUser().getId(),
                        r.getStartAt(),
                        r.getEndAt(),
                        r.getCreatedAt()
                ))
                .toList();

        LaboratoryResponse base = new LaboratoryResponse(
                lab.getId(), lab.getName(), lab.getBlock(),
                reserved, lab.getCreatedAt(), lab.getUpdatedAt()
        );
        return new LaboratoryDetailResponse(base, reservations);
    }

    /**
     * Creates a new laboratory.
     *
     * @throws DuplicateNameException if a laboratory with the same name already exists
     */
    @Transactional
    public LaboratoryResponse create(LaboratoryRequest request) {
        if (laboratoryRepository.findByName(request.name()).isPresent()) {
            throw new DuplicateNameException("Já existe um laboratório com o nome: " + request.name());
        }

        Laboratory lab = new Laboratory(request.name(), request.block());
        lab = laboratoryRepository.save(lab);

        Instant now = Instant.now();
        return toResponse(lab, now);
    }

    /**
     * Updates an existing laboratory.
     *
     * @throws ResourceNotFoundException if laboratory not found
     * @throws DuplicateNameException    if another laboratory already has the new name
     */
    @Transactional
    public LaboratoryResponse update(UUID id, LaboratoryRequest request) {
        Laboratory lab = laboratoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Laboratório não encontrado com id: " + id));

        if (laboratoryRepository.existsByNameAndIdNot(request.name(), id)) {
            throw new DuplicateNameException("Já existe um laboratório com o nome: " + request.name());
        }

        lab.setName(request.name());
        lab.setBlock(request.block());
        lab = laboratoryRepository.save(lab);

        Instant now = Instant.now();
        return toResponse(lab, now);
    }

    /**
     * Deletes a laboratory if it has no future reservations.
     *
     * @throws ResourceNotFoundException                 if laboratory not found
     * @throws LaboratoryHasFutureReservationsException  if there are future reservations
     */
    @Transactional
    public void delete(UUID id) {
        Laboratory lab = laboratoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Laboratório não encontrado com id: " + id));

        if (reservationRepository.existsFutureReservation(id, Instant.now())) {
            throw new LaboratoryHasFutureReservationsException(
                    "O laboratório possui reservas futuras e não pode ser excluído");
        }

        laboratoryRepository.delete(lab);
    }

    // --- helpers ---

    private LaboratoryResponse toResponse(Laboratory lab, Instant now) {
        boolean reserved = reservationRepository.existsFutureReservation(lab.getId(), now);
        return new LaboratoryResponse(
                lab.getId(), lab.getName(), lab.getBlock(),
                reserved, lab.getCreatedAt(), lab.getUpdatedAt()
        );
    }
}
