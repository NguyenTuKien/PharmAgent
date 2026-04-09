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
import ct01.n07.backend.model.enums.Gender;
import ct01.n07.backend.model.enums.MessageStatus;
import ct01.n07.backend.model.enums.RelationStatus;
import ct01.n07.backend.model.Message;
import ct01.n07.backend.repository.RelationshipRepository;
import ct01.n07.backend.repository.MessageRepository;
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
    private final RelationshipRepository relationshipRepository;
    private final MessageRepository messageRepository;

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
    public DoseEventResponse confirmDose(String id) {
        log.info("Elderly confirming dose event id={}", id);
        DoseEvent doseEvent = doseEventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy cữ thuốc với ID: " + id));

        MedicationResponse medication = patientMedicationService.getPatientMedicationById(doseEvent.getPatientMedicationId());
        UserProfile elderlyProfile = userProfileService.findById(medication.getPatientId());

        LocalDateTime now = LocalDateTime.now();

        // API xác nhận: Nếu đã quá hạn uống thì cập nhật là OVERDUE thay vì accept (TAKEN)
        if (now.isAfter(doseEvent.getScheduledAt())) {
            doseEvent.setStatus(DoseStatus.OVERDUE);
        } else {
            doseEvent.setStatus(DoseStatus.TAKEN);
        }

        doseEvent.setTakenAt(now);
        doseEvent.setConfirmedBy(elderlyProfile.getId());

        DoseEvent saved = doseEventRepository.save(doseEvent);

        // --- Gửi tin nhắn thông báo cho người chăm sóc ---
        try {
            String elderlyTitle = elderlyProfile.getGender() == Gender.MALE ? "Ông" : 
                                 elderlyProfile.getGender() == Gender.FEMALE ? "Bà" : "";
            
            String timeStr = now.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
            String content = String.format("%s %s đã uống thuốc %s vào lúc %s", 
                    elderlyTitle, elderlyProfile.getFirstName(), medication.getNickname(), timeStr);

            List<ct01.n07.backend.model.Relationship> relationships = relationshipRepository
                    .findAllByElderlyIdAndStatus(elderlyProfile.getId(), RelationStatus.ACCEPTED);

            List<Message> notifications = relationships.stream()
                    .map(rel -> Message.builder()
                            .senderId(elderlyProfile.getId())
                            .receiverId(rel.getCaregiverId())
                            .content(content)
                            .status(MessageStatus.SUCCESS)
                            .build())
                    .toList();

            if (!notifications.isEmpty()) {
                messageRepository.saveAll(notifications);
                log.info("Sent dose confirmation notifications to {} caregivers", notifications.size());
            }
        } catch (Exception e) {
            log.error("Failed to send dose confirmation notifications", e);
            // Không throw exception để tránh rollback việc confirm thuốc
        }

        return doseEventMapper.toResponse(saved);
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
}
