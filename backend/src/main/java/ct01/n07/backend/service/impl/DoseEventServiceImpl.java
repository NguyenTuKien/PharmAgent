package ct01.n07.backend.service.impl;

import ct01.n07.backend.dto.doseEvent.DoseEventResponse;
import ct01.n07.backend.dto.doseEvent.DoseStatusUpdateRequest;
import ct01.n07.backend.dto.patientMedication.MedicationResponse;
import ct01.n07.backend.mapper.DoseEventMapper;
import ct01.n07.backend.model.DoseEvent;
import ct01.n07.backend.model.UserProfile;
import ct01.n07.backend.repository.DoseEventRepository;
import ct01.n07.backend.service.DoseEventService;
import ct01.n07.backend.service.PatientMedicationService;
import ct01.n07.backend.service.UserProfileService;
import ct01.n07.backend.model.enums.DoseStatus;

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
public class DoseEventServiceImpl implements DoseEventService {

    private final DoseEventRepository doseEventRepository;
    private final PatientMedicationService patientMedicationService;
    private final DoseEventMapper doseEventMapper;
    private final UserProfileService userProfileService;

    @Override
    public List<DoseEvent> getAllDoseEvents() {
        return doseEventRepository.findAll();
    }

    @Override
    public DoseEvent getDoseEventById(String id) {
        return doseEventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy cữ thuốc với ID: " + id));
    }

    @Override
    public DoseEvent saveDoseEvent(DoseEvent doseEvent) {
        return doseEventRepository.save(doseEventMapper.toModel(doseEvent));
    }

    @Override
    public void deleteDoseEvent(String id) {
        doseEventRepository.deleteById(id);
    }

    @Override
    public Page<DoseEventResponse> getTodayDoses(String patientId, Pageable pageable) {
        log.info("Fetching today's dose doses for patientId={}", patientId);
        List<String> medicationIds = getMedicationIdsForPatient(patientId);
        if (medicationIds.isEmpty()) return Page.empty(pageable);

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        return doseEventRepository.findByPatientMedicationIdInAndScheduledAtBetweenOrderByScheduledAtAsc(
                medicationIds, startOfDay, endOfDay, pageable)
                .map(doseEventMapper::toResponse);
    }

    @Override
    public Page<DoseEventResponse> getPendingDoses(String patientId, Pageable pageable) {
        log.info("Fetching pending doses for patientId={}", patientId);
        List<String> medicationIds = getMedicationIdsForPatient(patientId);
        if (medicationIds.isEmpty()) return Page.empty(pageable);

        return doseEventRepository.findByPatientMedicationIdInAndStatus(
                medicationIds, DoseStatus.PENDING, pageable)
                .map(doseEventMapper::toResponse);
    }

    @Override
    public Page<DoseEventResponse> getProcessedDoses(String patientId, Pageable pageable) {
        log.info("Fetching processed doses for patientId={}", patientId);
        List<String> medicationIds = getMedicationIdsForPatient(patientId);
        if (medicationIds.isEmpty()) return Page.empty(pageable);

        return doseEventRepository.findByPatientMedicationIdInAndStatusNot(
                medicationIds, DoseStatus.PENDING, pageable)
                .map(doseEventMapper::toResponse);
    }

    private List<String> getMedicationIdsForPatient(String patientId) {
        List<MedicationResponse> medications = patientMedicationService.getMedications(patientId, null, Pageable.unpaged()).getContent();
        if (medications == null) return List.of();
        return medications.stream()
                .map(MedicationResponse::getId)
                .toList();
    }



    @Override
    public DoseEventResponse updateDoseStatus(String id, DoseStatusUpdateRequest request) {
        log.info("Updating dose event status id={}, newStatus={}", id, request.getStatus());

        DoseEvent doseEvent = doseEventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy cữ thuốc với ID: " + id));

        // Lấy profile của người đang thực hiện xác nhận
        UserProfile confirmedByProfile = userProfileService.getCurrentUserProfile();

        doseEvent.setStatus(request.getStatus());
        doseEvent.setNote(request.getNote());
        doseEvent.setConfirmedBy(confirmedByProfile.getId());

        switch (request.getStatus()) {
            case TAKEN -> {
                if (request.getTakenAt() != null) {
                    doseEvent.setTakenAt(request.getTakenAt());
                } else {
                    doseEvent.setTakenAt(LocalDateTime.now());
                }
            }
            case SKIPPED, MISSED -> doseEvent.setTakenAt(null);
            default -> {
                /* PENDING / OVERDUE: không thay đổi takenAt */ }
        }

        DoseEvent saved = doseEventRepository.save(doseEvent);
        return doseEventMapper.toResponse(saved);
    }

    @Override
    public long countTotalDoseEvents(List<String> patientMedicationIds, LocalDateTime startTime, LocalDateTime endTime) {
        return doseEventRepository.countByPatientMedicationIdInAndScheduledAtBetween(patientMedicationIds, startTime, endTime);
    }

    @Override
    public java.util.Map<DoseStatus, Long> countDoseEventsByStatus(List<String> patientMedicationIds, LocalDateTime startTime, LocalDateTime endTime) {
        java.util.Map<DoseStatus, Long> result = new java.util.EnumMap<>(DoseStatus.class);
        for (DoseStatus status : DoseStatus.values()) {
            result.put(status, doseEventRepository.countByPatientMedicationIdInAndScheduledAtBetweenAndStatus(
                    patientMedicationIds, startTime, endTime, status));
        }
        return result;
    }
}
