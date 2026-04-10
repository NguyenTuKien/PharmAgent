package ct01.n07.backend.service.impl;

import ct01.n07.backend.dto.event.EventDoseResponse;
import ct01.n07.backend.dto.event.DoseStatusUpdateRequest;
import ct01.n07.backend.mapper.EventDoseMapper;
import ct01.n07.backend.model.EventDose;
import ct01.n07.backend.model.UserProfile;
import ct01.n07.backend.model.enums.DoseStatus;
import ct01.n07.backend.repository.EventDoseRepository;
import ct01.n07.backend.security.MedicationPermissionValidator;
import ct01.n07.backend.security.ProfileAccessContext;
import ct01.n07.backend.service.EventDoseService;
import ct01.n07.backend.service.MedicationCoreService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventDoseServiceImpl implements EventDoseService {

    private final EventDoseRepository eventDoseRepository;
    private final MedicationCoreService medicationCoreService;
    private final EventDoseMapper eventDoseMapper;
    private final ProfileAccessContext profileAccessContext;
    private final MedicationPermissionValidator permissionValidator;

    @Override
    public List<EventDose> getAllDoseEvents() {
        return eventDoseRepository.findAll();
    }

    @Override
    public EventDose getEventDoseById(String id) {
        return eventDoseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Khong tim thay cu thuoc voi ID: " + id));
    }

    @Override
    public EventDose saveEventDose(EventDose eventDose) {
        return eventDoseRepository.save(eventDoseMapper.toModel(eventDose));
    }

    @Override
    public void deleteEventDose(String id) {
        eventDoseRepository.deleteById(id);
    }

    @Override
    public Page<EventDoseResponse> getTodayDoses(String patientId, Pageable pageable) {
        log.info("Fetching today's dose doses for patientId={}", patientId);
        validateAccessToPatient(patientId);

        List<String> medicationIds = getMedicationIdsForPatient(patientId);
        if (medicationIds.isEmpty()) return Page.empty(pageable);

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        return eventDoseRepository.findByMedicationIdInAndScheduledAtBetweenOrderByScheduledAtAsc(
                medicationIds, startOfDay, endOfDay, pageable)
                .map(eventDoseMapper::toResponse);
    }

    @Override
    public Page<EventDoseResponse> getPendingDoses(String patientId, Pageable pageable) {
        log.info("Fetching pending doses for patientId={}", patientId);
        validateAccessToPatient(patientId);

        List<String> medicationIds = getMedicationIdsForPatient(patientId);
        if (medicationIds.isEmpty()) return Page.empty(pageable);

        return eventDoseRepository.findByMedicationIdInAndStatus(
                medicationIds, DoseStatus.PENDING, pageable)
                .map(eventDoseMapper::toResponse);
    }

    @Override
    public Page<EventDoseResponse> getProcessedDoses(String patientId, Pageable pageable) {
        log.info("Fetching processed doses for patientId={}", patientId);
        validateAccessToPatient(patientId);

        List<String> medicationIds = getMedicationIdsForPatient(patientId);
        if (medicationIds.isEmpty()) return Page.empty(pageable);

        return eventDoseRepository.findByMedicationIdInAndStatusNot(
                medicationIds, DoseStatus.PENDING, pageable)
                .map(eventDoseMapper::toResponse);
    }

    private List<String> getMedicationIdsForPatient(String patientId) {
        return medicationCoreService.getMedicationIdsByPatientId(patientId);
    }

    private void validateAccessToPatient(String patientId) {
        UserProfile currentProfile = profileAccessContext.getCurrentUserProfile();
        permissionValidator.verifyAccessToPatient(currentProfile.getRole(), currentProfile.getId(), patientId);
    }

    @Override
    public EventDoseResponse updateDoseStatus(String id, DoseStatusUpdateRequest request) {
        log.info("Updating dose stats status id={}, newStatus={}", id, request.getStatus());

        EventDose eventDose = eventDoseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Khong tim thay cu thuoc voi ID: " + id));

        // getMedicationById verifies the current user has access to this medication's patient
        medicationCoreService.getMedicationById(eventDose.getMedicationId());

        UserProfile confirmedByProfile = profileAccessContext.getCurrentUserProfile();

        eventDose.setStatus(request.getStatus());
        eventDose.setNote(request.getNote());
        eventDose.setConfirmedBy(confirmedByProfile.getId());

        switch (request.getStatus()) {
            case TAKEN -> {
                if (request.getTakenAt() != null) {
                    eventDose.setTakenAt(request.getTakenAt());
                } else {
                    eventDose.setTakenAt(LocalDateTime.now());
                }
            }
            case SKIPPED, MISSED -> eventDose.setTakenAt(null);
            default -> {
                /* PENDING / OVERDUE: keep takenAt unchanged */
            }
        }

        EventDose saved = eventDoseRepository.save(eventDose);
        return eventDoseMapper.toResponse(saved);
    }

    @Override
    public long countTotalDoseEvents(List<String> medicationIds, LocalDateTime startTime, LocalDateTime endTime) {
        return eventDoseRepository.countByMedicationIdInAndScheduledAtBetween(medicationIds, startTime, endTime);
    }

    @Override
    public java.util.Map<DoseStatus, Long> countDoseEventsByStatus(List<String> medicationIds, LocalDateTime startTime, LocalDateTime endTime) {
        java.util.Map<DoseStatus, Long> result = new java.util.EnumMap<>(DoseStatus.class);
        for (DoseStatus status : DoseStatus.values()) {
            result.put(status, eventDoseRepository.countByMedicationIdInAndScheduledAtBetweenAndStatus(
                    medicationIds, startTime, endTime, status));
        }
        return result;
    }
}


