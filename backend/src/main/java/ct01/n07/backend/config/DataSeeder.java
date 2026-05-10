package ct01.n07.backend.config;

import ct01.n07.backend.model.*;
import ct01.n07.backend.model.enums.*;
import ct01.n07.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final PillRepository pillRepository;
    private final MedicationRepository medicationRepository;
    
    private final RelationshipRepository relationshipRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final CallLogRepository callLogRepository;
    private final NotificationRepository notificationRepository;
    private final EventDoseRepository eventDoseRepository;

    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        log.info("== Seed sample data started ==");

        User admin = upsertUser("admin@pharmagent.local", "Admin@123", UserStatus.ACTIVE);
        User elderly = upsertUser("elderly@pharmagent.local", "Elderly@123", UserStatus.ACTIVE);
        User caregiver = upsertUser("caregiver@pharmagent.local", "Caregiver@123", UserStatus.ACTIVE);

        upsertAdminProfile(admin);
        upsertElderlyProfile(elderly);
        upsertCaregiverProfile(caregiver);

        upsertDevice(elderly, "fcm_elderly_123", "iPad cua Ong", DeviceType.IOS);
        upsertDevice(caregiver, "fcm_caregiver_123", "iPhone cua Con", DeviceType.IOS);

        upsertRelationship(caregiver, elderly);
        upsertChatAndCall(caregiver, elderly);
        upsertNotification(caregiver, elderly);

        Pill paracetamol = upsertParacetamol();
        upsertAmlodipine();

        Medication medication = upsertMedication(elderly, paracetamol);
        upsertEventDose(elderly, medication);

        log.info("== Seed sample data completed ==");
    }

    private User upsertUser(String email, String rawPassword, UserStatus status) {
        User user = userRepository.findByEmail(email).orElseGet(User::new);
        if (user.getId() == null) {
            user.setCreatedAt(Instant.now());
        }

        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setUserStatus(status);

        return userRepository.save(user);
    }

    private void upsertAdminProfile(User admin) {
        upsertProfile(admin, Role.ADMIN, profile -> {
            profile.setFirstName("Quan tri vien");
            profile.setLastName(null);
            profile.setPhone("0900000001");
            profile.setDateOfBirth(null);
            profile.setGender(null);
            profile.setAddress(null);
            profile.setAvatarUrl(null);
            profile.setUserContacts(List.of());
        });
    }

    private void upsertElderlyProfile(User elderly) {
        upsertProfile(elderly, Role.ELDERLY, profile -> {
            profile.setFirstName("Kien");
            profile.setLastName("Nguyen");
            profile.setPhone("0900000002");
            profile.setDateOfBirth(LocalDate.of(1950, 5, 15));
            profile.setGender(Gender.MALE);
            profile.setAddress("Ha Noi, Viet Nam");
            profile.setAvatarUrl(null);
            profile.setUserContacts(List.of(
                    UserContact.builder()
                            .name("Con trai Dai")
                            .phone("0987654321")
                            .build()));
        });
    }

    private void upsertCaregiverProfile(User caregiver) {
        upsertProfile(caregiver, Role.CAREGIVER, profile -> {
            profile.setFirstName("Anh");
            profile.setLastName("Nguyen");
            profile.setPhone("0900000003");
            profile.setDateOfBirth(LocalDate.of(1988, 8, 20));
            profile.setGender(Gender.MALE);
            profile.setAddress("Ha Noi, Viet Nam");
            profile.setAvatarUrl(null);
            profile.setUserContacts(List.of());
        });
    }

    private UserProfile upsertProfile(User user, Role role, Consumer<UserProfile> updater) {
        UserProfile profile = findProfileByUserIdAndRole(user.getId(), role)
                .orElseGet(() -> {
                    UserProfile newProfile = new UserProfile();
                    newProfile.setCreatedAt(Instant.now());
                    newProfile.setUserId(user.getId());
                    newProfile.setRole(role);
                    return newProfile;
                });

        updater.accept(profile);
        profile.setUserId(user.getId());
        profile.setRole(role);
        return userProfileRepository.save(profile);
    }

    private Optional<UserProfile> findProfileByUserIdAndRole(String userId, Role role) {
        return userProfileRepository.findAllByUserId(userId, PageRequest.of(0, 100)).getContent().stream()
                .filter(profile -> role.equals(profile.getRole()))
                .min(Comparator.comparing(UserProfile::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())));
    }

    private void upsertDevice(User user, String token, String name, DeviceType type) {
        if (userDeviceRepository.findByDeviceToken(token).isEmpty()) {
            UserDevice device = UserDevice.builder()
                    .userId(user.getId())
                    .deviceName(name)
                    .deviceToken(token)
                    .deviceType(type)
                    .isActive(true)
                    .lastSeenAt(Instant.now())
                    .build();
            userDeviceRepository.save(device);
        }
    }

    private void upsertRelationship(User caregiver, User elderly) {
        if (!relationshipRepository.existsByCaregiverIdAndElderlyId(caregiver.getId(), elderly.getId())) {
            Relationship relationship = Relationship.builder()
                    .caregiverId(caregiver.getId())
                    .elderlyId(elderly.getId())
                    .caregiverTitle("Con trai")
                    .elderlyTitle("Bố")
                    .permissionLevel(PermissionLevel.MANAGE_ALL)
                    .status(RelationStatus.ACCEPTED)
                    .startDate(LocalDate.now().minusDays(10))
                    .createdAt(Instant.now())
                    .build();
            relationshipRepository.save(relationship);
        }
    }

    private void upsertChatAndCall(User caregiver, User elderly) {
        // ChatRoom
        ChatRoom room = chatRoomRepository.findByTypeAndParticipantIdsContainingAndParticipantIdsContaining(
                "DIRECT", caregiver.getId(), elderly.getId()
        ).orElseGet(() -> {
            ChatRoom newRoom = ChatRoom.builder()
                    .type("DIRECT")
                    .participantIds(List.of(caregiver.getId(), elderly.getId()))
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            return chatRoomRepository.save(newRoom);
        });

        // ChatMessage
        if (chatMessageRepository.count() == 0) {
            ChatMessage msg1 = ChatMessage.builder()
                    .roomId(room.getId())
                    .senderId(caregiver.getId())
                    .content("Bố nhớ uống thuốc nhé!")
                    .type("TEXT")
                    .readBy(List.of(caregiver.getId(), elderly.getId()))
                    .sentAt(Instant.now().minus(1, ChronoUnit.HOURS))
                    .build();
            ChatMessage msg2 = ChatMessage.builder()
                    .roomId(room.getId())
                    .senderId(elderly.getId())
                    .content("Bố uống rồi con nhé.")
                    .type("TEXT")
                    .readBy(List.of(caregiver.getId(), elderly.getId()))
                    .sentAt(Instant.now().minus(30, ChronoUnit.MINUTES))
                    .build();
            chatMessageRepository.saveAll(List.of(msg1, msg2));

            room.setLastMessageId(msg2.getId());
            chatRoomRepository.save(room);
        }

        // CallLog
        if (callLogRepository.findByCallerIdOrReceiverId(caregiver.getId(), elderly.getId()).isEmpty()) {
            CallLog call = CallLog.builder()
                    .callerId(caregiver.getId())
                    .receiverId(elderly.getId())
                    .startedAt(Instant.now().minus(2, ChronoUnit.HOURS))
                    .endedAt(Instant.now().minus(1, ChronoUnit.HOURS).minus(50, ChronoUnit.MINUTES))
                    .durationInSeconds(600L)
                    .status("COMPLETED")
                    .build();
            callLogRepository.save(call);
        }
    }

    private void upsertNotification(User caregiver, User elderly) {
        if (notificationRepository.count() == 0) {
            Notification notif = Notification.builder()
                    .senderId(caregiver.getId())
                    .receiverId(elderly.getId())
                    .content("Tài khoản của bạn đã được kết nối với người chăm sóc.")
                    .sentAt(Instant.now())
                    .status(NotificationStatus.SUCCESS)
                    .build();
            notificationRepository.save(notif);
        }
    }

    private Pill upsertParacetamol() {
        Pill pill = pillRepository.findByNameAndStrength("Paracetamol 500mg", "500mg").orElseGet(Pill::new);
        if (pill.getId() == null) {
            pill.setCreatedAt(Instant.now());
        }

        pill.setName("Paracetamol 500mg");
        pill.setGenericName("Paracetamol");
        pill.setBrandName("Panadol");
        pill.setStrength("500mg");
        pill.setDosageForm("Vien nen");
        pill.setColor("Trang");
        pill.setShape("Tron");
        pill.setDescription("Thuoc giam dau ha sot");
        pill.setUsageInstructions("Uong sau khi an no. Khong uong qua 4 vien/ngay.");
        pill.setWarning("Khong dung cho nguoi suy gan");
        pill.setSideEffects("Co the gay buon ngu nhe.");
        pill.setManufacturer("GlaxoSmithKline");
        pill.setActive(true);
        pill.setImages(List.of(
                PillImage.builder()
                        .imageUrl("https://example.com/panadol-front.jpg")
                        .viewType(ViewType.FRONT)
                        .isPrimary(true)
                        .build()));

        return pillRepository.save(pill);
    }

    private void upsertAmlodipine() {
        Pill pill = pillRepository.findByNameAndStrength("Amlodipine 5mg", "5mg").orElseGet(Pill::new);
        if (pill.getId() == null) {
            pill.setCreatedAt(Instant.now());
        }

        pill.setName("Amlodipine 5mg");
        pill.setGenericName("Amlodipine");
        pill.setBrandName("Amlor");
        pill.setStrength("5mg");
        pill.setDosageForm("Vien nang");
        pill.setColor("Vang");
        pill.setShape(null);
        pill.setDescription("Thuoc huyet ap");
        pill.setUsageInstructions("Uong vao buoi sang, truoc hoac sau an deu duoc.");
        pill.setWarning(null);
        pill.setSideEffects(null);
        pill.setManufacturer(null);
        pill.setActive(true);
        pill.setImages(List.of());

        pillRepository.save(pill);
    }

    private Medication upsertMedication(User elderly, Pill paracetamol) {
        String nickname = "Thuoc dau dau (hop xanh)";
        Medication medication = medicationRepository
                .findByPatientIdAndPillIdAndNickname(elderly.getId(), paracetamol.getId(), nickname)
                .orElseGet(Medication::new);

        if (medication.getId() == null) {
            medication.setCreatedAt(Instant.now());
        }

        medication.setPatientId(elderly.getId());
        medication.setPillId(paracetamol.getId());
        medication.setNickname(nickname);
        medication.setDosageAmount(new BigDecimal("1.0"));
        medication.setDosageUnit("Vien");
        medication.setRoute("Uong");
        medication.setMealRelation(MealRelation.AFTER_MEAL);
        medication.setInstruction("Uong voi nhieu nuoc am");
        medication.setPrescribedBy("Bac si Tuan - Vien Tim mach");
        medication.setPurpose("Giam dau, ha sot");
        medication.setStartDate(LocalDate.now());
        medication.setEndDate(LocalDate.now().plusDays(7));
        medication.setActive(true);
        medication.setMedicationSchedules(List.of());

        return medicationRepository.save(medication);
    }

    private void upsertEventDose(User elderly, Medication medication) {
        if (eventDoseRepository.count() == 0) {
            EventDose event = EventDose.builder()
                    .medicationId(medication.getId())
                    .scheduledAt(LocalDateTime.now().minusHours(2))
                    .status(DoseStatus.TAKEN)
                    .takenAt(LocalDateTime.now().minusHours(1).minusMinutes(55))
                    .confirmedBy(elderly.getId())
                    .note("Uống sau khi ăn sáng")
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            eventDoseRepository.save(event);
        }
    }
}
