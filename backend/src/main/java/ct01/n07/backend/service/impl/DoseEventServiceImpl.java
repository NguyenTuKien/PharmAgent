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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    public List<DoseEventResponse> getTodayTimeline(String patientId, LocalDate date) {
        log.info("Fetching dose timeline for patientId={} on date={}", patientId, date);

        // Lấy danh sách ID của tất cả thuốc của bệnh nhân qua PatientMedicationService
        List<MedicationResponse> medications = patientMedicationService.getMedications(patientId, null, Pageable.unpaged()).getContent();
        if (medications == null || medications.isEmpty()) {
            return List.of();
        }

        List<String> medicationIds = medications.stream()
                .map(MedicationResponse::getId)
                .toList();

        // Truy vấn DoseEvents trong khung giờ của ngày được chỉ định
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        return doseEventRepository
                .findByPatientMedicationIdInAndScheduledAtBetweenOrderByScheduledAtAsc(
                        medicationIds, startOfDay, endOfDay)
                .stream()
                .map(doseEventMapper::toResponse)
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
            case TAKEN -> doseEvent.setTakenAt(LocalDateTime.now());
            case SKIPPED, MISSED -> doseEvent.setTakenAt(null);
            default -> {
                /* PENDING / OVERDUE: không thay đổi takenAt */ }
        }

        DoseEvent saved = doseEventRepository.save(doseEvent);
        return doseEventMapper.toResponse(saved);
    }
}
