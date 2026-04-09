package ct01.n07.backend.facade;

import ct01.n07.backend.dto.event.EventDoseResponse;
import ct01.n07.backend.dto.medication.MedicationResponse;
import ct01.n07.backend.exception.ForbiddenAccessException;
import ct01.n07.backend.mapper.EventDoseMapper;
import ct01.n07.backend.model.EventDose;
import ct01.n07.backend.model.Message;
import ct01.n07.backend.model.UserProfile;
import ct01.n07.backend.model.enums.DoseStatus;
import ct01.n07.backend.model.enums.Gender;
import ct01.n07.backend.model.enums.MessageStatus;
import ct01.n07.backend.service.EventDoseService;
import ct01.n07.backend.service.MedicationService;
import ct01.n07.backend.service.MessageService;
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

    private final EventDoseService eventDoseService;
    private final MedicationService medicationService;
    private final UserProfileService userProfileService;
    private final RelationshipService relationshipService;
    private final MessageService messageService;
    private final EventDoseMapper eventDoseMapper;

    @Transactional
    public EventDoseResponse confirmDose(String doseEventId) {
        log.info("Elderly confirming dose stats id={}", doseEventId);

        // Lấy thông tin cữ thuốc qua Core Service
        EventDose eventDose = eventDoseService.getEventDoseById(doseEventId);
        MedicationResponse medication = medicationService.getMedicationById(eventDose.getMedicationId());
        UserProfile elderlyProfile = userProfileService.findById(medication.getPatientId());

        UserProfile currentProfile = userProfileService.getCurrentUserProfile();
        if (!currentProfile.getId().equals(medication.getPatientId())) {
            throw new ForbiddenAccessException("Bạn không có quyền xác nhận uống thuốc cho người khác");
        }

        LocalDateTime now = LocalDateTime.now();

        if (now.isAfter(eventDose.getScheduledAt())) {
            eventDose.setStatus(DoseStatus.OVERDUE);
        } else {
            eventDose.setStatus(DoseStatus.TAKEN);
        }

        eventDose.setTakenAt(now);
        eventDose.setConfirmedBy(elderlyProfile.getId());

        // Lưu thông qua Core Service
        EventDose saved = eventDoseService.saveEventDose(eventDose);

        // Gửi notification
        sendDoseConfirmationNotifications(elderlyProfile, medication, now);

        return eventDoseMapper.toResponse(saved);
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
