package ct01.n07.backend.config;

import ct01.n07.backend.model.EmergencyContact;
import ct01.n07.backend.model.PatientMedication;
import ct01.n07.backend.model.Pill;
import ct01.n07.backend.model.PillImage;
import ct01.n07.backend.model.User;
import ct01.n07.backend.model.UserDevice;
import ct01.n07.backend.model.UserProfile;
import ct01.n07.backend.model.enums.DeviceType;
import ct01.n07.backend.model.enums.Gender;
import ct01.n07.backend.model.enums.MealRelation;
import ct01.n07.backend.model.enums.Role;
import ct01.n07.backend.model.enums.UserStatus;
import ct01.n07.backend.model.enums.ViewType;
import ct01.n07.backend.repository.PatientMedicationRepository;
import ct01.n07.backend.repository.PillRepository;
import ct01.n07.backend.repository.UserProfileRepository;
import ct01.n07.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
    private final PillRepository pillRepository;
    private final PatientMedicationRepository patientMedicationRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        log.info("== Seed sample data started ==");

        User admin = upsertUser("admin@pharmagent.local", "Admin@123", UserStatus.ACTIVE);
        User familyAccount = upsertUser("family@pharmagent.local", "Family@123", UserStatus.ACTIVE);

        upsertAdminProfile(admin);
        upsertElderlyProfile(familyAccount);
        upsertCaregiverProfile(familyAccount);

        Pill paracetamol = upsertParacetamol();
        upsertAmlodipine();

        upsertMedication(familyAccount, paracetamol);

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
            profile.setEmergencyContacts(List.of());
            profile.setUserDevices(List.of());
        });
    }

    private void upsertElderlyProfile(User familyAccount) {
        upsertProfile(familyAccount, Role.ELDERLY, profile -> {
            profile.setFirstName("Kien");
            profile.setLastName("Nguyen");
            profile.setPhone("0900000002");
            profile.setDateOfBirth(LocalDate.of(1950, 5, 15));
            profile.setGender(Gender.MALE);
            profile.setAddress("Ha Noi, Viet Nam");
            profile.setAvatarUrl(null);
            profile.setEmergencyContacts(List.of(
                    EmergencyContact.builder()
                            .name("Con trai Dai")
                            .phone("0987654321")
                            .build()));
            profile.setUserDevices(List.of(
                    UserDevice.builder()
                            .deviceName("iPad cua ong")
                            .deviceToken("fcm_token_sample_123")
                            .deviceType(DeviceType.IOS)
                            .isActive(true)
                            .lastSeenAt(Instant.now())
                            .build()));
        });
    }

    private void upsertCaregiverProfile(User familyAccount) {
        upsertProfile(familyAccount, Role.CAREGIVER, profile -> {
            profile.setFirstName("Anh");
            profile.setLastName("Nguyen");
            profile.setPhone("0900000003");
            profile.setDateOfBirth(LocalDate.of(1988, 8, 20));
            profile.setGender(Gender.MALE);
            profile.setAddress("Ha Noi, Viet Nam");
            profile.setAvatarUrl(null);
            profile.setEmergencyContacts(List.of());
            profile.setUserDevices(List.of());
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
        pill.setDescription(null);
        pill.setUsageInstructions("Uong sau khi an no. Khong uong qua 4 vien/ngay.");
        pill.setWarning(null);
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
        pill.setDescription(null);
        pill.setUsageInstructions("Uong vao buoi sang, truoc hoac sau an deu duoc.");
        pill.setWarning(null);
        pill.setSideEffects(null);
        pill.setManufacturer(null);
        pill.setActive(true);
        pill.setImages(List.of());

        pillRepository.save(pill);
    }

    private void upsertMedication(User familyAccount, Pill paracetamol) {
        String nickname = "Thuoc dau dau (hop xanh)";
        PatientMedication medication = patientMedicationRepository
                .findByPatientIdAndPillIdAndNickname(familyAccount.getId(), paracetamol.getId(), nickname)
                .orElseGet(PatientMedication::new);

        if (medication.getId() == null) {
            medication.setCreatedAt(Instant.now());
        }

        medication.setPatientId(familyAccount.getId());
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

        patientMedicationRepository.save(medication);
    }
}
