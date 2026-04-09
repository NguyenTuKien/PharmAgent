package ct01.n07.backend.facade;

import ct01.n07.backend.dto.doseEvent.DoseEventResponse;
import ct01.n07.backend.dto.patientMedication.MedicationResponse;
import ct01.n07.backend.mapper.DoseEventMapper;
import ct01.n07.backend.model.DoseEvent;
import ct01.n07.backend.model.Message;
import ct01.n07.backend.model.UserProfile;
import ct01.n07.backend.model.enums.DoseStatus;
import ct01.n07.backend.model.enums.Gender;
import ct01.n07.backend.model.enums.MessageStatus;
import ct01.n07.backend.service.DoseEventService;
import ct01.n07.backend.service.MessageService;
import ct01.n07.backend.service.PatientMedicationService;
import ct01.n07.backend.service.RelationshipService;
import ct01.n07.backend.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DoseConfirmationFacade {

    private final DoseEventService doseEventService;
    private final PatientMedicationService patientMedicationService;
    private final UserProfileService userProfileService;
    private final RelationshipService relationshipService;
    private final MessageService messageService;
    private final DoseEventMapper doseEventMapper;

    @Transactional
    public DoseEventResponse confirmDose(String doseEventId) {
        log.info("Elderly confirming dose event id={}", doseEventId);

        // Lấy thông tin cữ thuốc qua Core Service
        DoseEvent doseEvent = doseEventService.getDoseEventById(doseEventId);
        MedicationResponse medication = patientMedicationService.getPatientMedicationById(doseEvent.getPatientMedicationId());

        // Verify the current user is the owner of this dose event
        UserProfile currentProfile = userProfileService.getCurrentUserProfile();
        if (!currentProfile.getId().equals(medication.getPatientId())) {
            throw new ct01.n07.backend.exception.ForbiddenAccessException(
                    "Bạn không có quyền xác nhận cữ thuốc của người khác");
        }
        UserProfile elderlyProfile = currentProfile;

        LocalDateTime now = LocalDateTime.now();

        if (now.isAfter(doseEvent.getScheduledAt())) {
            doseEvent.setStatus(DoseStatus.OVERDUE);
        } else {
            doseEvent.setStatus(DoseStatus.TAKEN);
        }

        doseEvent.setTakenAt(now);
        doseEvent.setConfirmedBy(elderlyProfile.getId());

        // Lưu thông qua Core Service
        DoseEvent saved = doseEventService.saveDoseEvent(doseEvent);

        // Gửi notification
        sendDoseConfirmationNotifications(elderlyProfile, medication, now);

        return doseEventMapper.toResponse(saved);
    }

    private void sendDoseConfirmationNotifications(UserProfile elderlyProfile, MedicationResponse medication, LocalDateTime time) {
        try {
            String elderlyTitle = elderlyProfile.getGender() == Gender.MALE ? "Ông" :
                    elderlyProfile.getGender() == Gender.FEMALE ? "Bà" : "";

            String timeStr = time.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
            String content = String.format("%s %s đã uống thuốc %s vào lúc %s",
                    elderlyTitle, elderlyProfile.getFirstName(), medication.getNickname(), timeStr);

            // Fetch relations via RelationshipService
            List<ct01.n07.backend.model.Relationship> relationships = relationshipService
                    .getAcceptedCaregiverRelationshipsByElderly(elderlyProfile.getId());

            List<Message> notifications = relationships.stream()
                    .map(rel -> Message.builder()
                            .senderId(elderlyProfile.getId())
                            .receiverId(rel.getCaregiverId())
                            .content(content)
                            .status(MessageStatus.SUCCESS)
                            .build())
                    .toList();

            if (!notifications.isEmpty()) {
                // Save messages via MessageService
                messageService.saveAllMessages(notifications);
                log.info("Sent dose confirmation notifications to {} caregivers", notifications.size());
            }
        } catch (Exception e) {
            log.error("Failed to send dose confirmation notifications", e);
        }
    }
}
