package ct01.n07.backend.service.impl;

import ct01.n07.backend.dto.patientMedication.MedicationCreateRequest;
import ct01.n07.backend.dto.patientMedication.MedicationResponse;
import ct01.n07.backend.dto.patientMedication.MedicationScheduleRequest;
import ct01.n07.backend.dto.patientMedication.MedicationUpdateRequest;
import ct01.n07.backend.dto.patientMedication.ScheduleTimeRequest;
import ct01.n07.backend.mapper.PatientMedicationMapper;
import ct01.n07.backend.model.MedicationSchedule;
import ct01.n07.backend.model.PatientMedication;
import ct01.n07.backend.model.ScheduleTime;
import ct01.n07.backend.model.UserProfile;
import ct01.n07.backend.model.enums.RelationStatus;
import ct01.n07.backend.model.enums.Role;
import ct01.n07.backend.model.enums.ScheduleType;
import ct01.n07.backend.repository.PatientMedicationRepository;
import ct01.n07.backend.repository.PillRepository;
import ct01.n07.backend.repository.RelationshipRepository;
import ct01.n07.backend.repository.UserProfileRepository;
import ct01.n07.backend.service.PatientMedicationService;
import ct01.n07.backend.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PatientMedicationServiceImpl implements PatientMedicationService {
    private final PatientMedicationRepository patientMedicationRepository;
    private final PillRepository pillRepository;
    private final UserProfileRepository userProfileRepository;
    private final RelationshipRepository relationshipRepository;
    private final UserProfileService userProfileService;
    private final PatientMedicationMapper patientMedicationMapper;

    @Override
    public MedicationResponse createMedication(MedicationCreateRequest request) {
        UserProfile currentProfile = userProfileService.getCurrentUserProfile();
        requireRole(currentProfile, Role.CAREGIVER, Role.ELDERLY);

        if (currentProfile.getRole() == Role.ELDERLY && !currentProfile.getId().equals(request.getPatientId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Elderly can only create medication for their own profile");
        }

        if (!userProfileRepository.existsByIdAndRole(request.getPatientId(), Role.ELDERLY)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Patient profile is invalid");
        }

        if (!pillRepository.existsById(request.getPillId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pill not found");
        }

        verifySchedulePermission(currentProfile, request.getPatientId());

        PatientMedication medication = PatientMedication.builder()
                .patientId(request.getPatientId())
                .pillId(request.getPillId())
                .nickname(request.getNickname())
                .dosageAmount(request.getDosageAmount())
                .dosageUnit(request.getDosageUnit())
                .route(request.getRoute())
                .mealRelation(request.getMealRelation())
                .instruction(request.getInstruction())
                .prescribedBy(request.getPrescribedBy())
                .purpose(request.getPurpose())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .isPrn(request.isPrn())
                .maxPerDay(request.getMaxPerDay())
                .totalQuantity(request.getTotalQuantity())
                .isActive(true)
                .medicationSchedules(List.of(toSimpleDailySchedule(request.getSchedules())))
                .build();

        return toMedicationResponse(patientMedicationRepository.save(medication));
    }

    @Override
    public Page<MedicationResponse> getMedications(String patientId, Boolean isActive, Pageable pageable) {
        UserProfile currentProfile = userProfileService.getCurrentUserProfile();
        requireRole(currentProfile, Role.CAREGIVER, Role.ELDERLY);
        verifyAccessToPatient(currentProfile, patientId);

        Page<PatientMedication> page = isActive == null
                ? patientMedicationRepository.findByPatientId(patientId, pageable)
                : patientMedicationRepository.findByPatientIdAndIsActive(patientId, isActive, pageable);

        return page.map(this::toMedicationResponse);
    }

    @Override
    public MedicationResponse updateMedication(String id, MedicationUpdateRequest request) {
        UserProfile currentProfile = userProfileService.getCurrentUserProfile();
        requireRole(currentProfile, Role.CAREGIVER, Role.ELDERLY);

        PatientMedication medication = requirePatientMedication(id);

        verifySchedulePermission(currentProfile, medication.getPatientId());

        if (request.getPillId() != null) {
            if (!pillRepository.existsById(request.getPillId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pill not found");
            }
            medication.setPillId(request.getPillId());
        }

        if (request.getSchedules() != null) {
            medication.setMedicationSchedules(List.of(toSimpleDailySchedule(request.getSchedules())));
        }

        if (request.getTotalQuantity() != null) {
            medication.setTotalQuantity(request.getTotalQuantity());
        }

        if (request.getIsActive() != null) {
            medication.setActive(request.getIsActive());
        }

        if (request.getNickname() != null)
            medication.setNickname(request.getNickname());
        if (request.getDosageAmount() != null)
            medication.setDosageAmount(request.getDosageAmount());
        if (request.getDosageUnit() != null)
            medication.setDosageUnit(request.getDosageUnit());
        if (request.getRoute() != null)
            medication.setRoute(request.getRoute());
        if (request.getMealRelation() != null)
            medication.setMealRelation(request.getMealRelation());
        if (request.getInstruction() != null)
            medication.setInstruction(request.getInstruction());
        if (request.getPrescribedBy() != null)
            medication.setPrescribedBy(request.getPrescribedBy());
        if (request.getPurpose() != null)
            medication.setPurpose(request.getPurpose());
        if (request.getStartDate() != null)
            medication.setStartDate(request.getStartDate());
        if (request.getEndDate() != null)
            medication.setEndDate(request.getEndDate());
        if (request.getIsPrn() != null)
            medication.setPrn(request.getIsPrn());
        if (request.getMaxPerDay() != null)
            medication.setMaxPerDay(request.getMaxPerDay());

        return toMedicationResponse(patientMedicationRepository.save(medication));
    }

    @Override
    public MedicationResponse getPatientMedicationById(String id) {
        return toMedicationResponse(requirePatientMedication(id));
    }

    @Override
    public MedicationResponse addMedicationSchedule(MedicationScheduleRequest medicationScheduleRequest) {
        PatientMedication patientMedication = requirePatientMedication(
                medicationScheduleRequest.getPatientMedicationId());

        UserProfile currentProfile = userProfileService.getCurrentUserProfile();
        verifySchedulePermission(currentProfile, patientMedication.getPatientId());

        if (patientMedication.getMedicationSchedules() == null) {
            patientMedication.setMedicationSchedules(new ArrayList<>());
        }

        patientMedication.getMedicationSchedules().add(patientMedicationMapper.toModel(medicationScheduleRequest));
        return toMedicationResponse(patientMedicationRepository.save(patientMedication));
    }

    @Override
    public void deletePatientMedication(String id) {
        PatientMedication pm = requirePatientMedication(id);
        UserProfile currentProfile = userProfileService.getCurrentUserProfile();
        verifySchedulePermission(currentProfile, pm.getPatientId());
        
        patientMedicationRepository.deleteById(id);
    }

    @Override
    public MedicationResponse updateMedicationSchedule(String patientMedicationId, String scheduleId,
            MedicationScheduleRequest request) {
        PatientMedication pm = requirePatientMedication(patientMedicationId);

        UserProfile currentProfile = userProfileService.getCurrentUserProfile();
        verifySchedulePermission(currentProfile, pm.getPatientId());

        List<MedicationSchedule> schedules = pm.getMedicationSchedules();
        if (schedules == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found");
        }

        MedicationSchedule scheduleToUpdate = schedules.stream()
                .filter(s -> Objects.equals(s.getId(), scheduleId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found"));

        patientMedicationMapper.updateModel(scheduleToUpdate, request);
        return toMedicationResponse(patientMedicationRepository.save(pm));
    }

    @Override
    public MedicationResponse deleteMedicationSchedule(String patientMedicationId, String scheduleId) {
        PatientMedication pm = requirePatientMedication(patientMedicationId);

        UserProfile currentProfile = userProfileService.getCurrentUserProfile();
        verifySchedulePermission(currentProfile, pm.getPatientId());

        List<MedicationSchedule> schedules = pm.getMedicationSchedules();
        if (schedules == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found");
        }
        boolean removed = schedules.removeIf(s -> Objects.equals(s.getId(), scheduleId));
        if (!removed) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found");
        }
        return toMedicationResponse(patientMedicationRepository.save(pm));
    }

    @Override
    public MedicationResponse addScheduleTime(String patientMedicationId, String scheduleId,
            ScheduleTimeRequest request) {
        PatientMedication pm = requirePatientMedication(patientMedicationId);

        UserProfile currentProfile = userProfileService.getCurrentUserProfile();
        verifySchedulePermission(currentProfile, pm.getPatientId());

        List<MedicationSchedule> schedules = pm.getMedicationSchedules();
        if (schedules == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found");
        }

        MedicationSchedule schedule = schedules.stream()
                .filter(s -> Objects.equals(s.getId(), scheduleId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found"));

        if (schedule.getScheduleTimeList() == null) {
            schedule.setScheduleTimeList(new ArrayList<>());
        }

        schedule.getScheduleTimeList().add(patientMedicationMapper.toModel(request));
        return toMedicationResponse(patientMedicationRepository.save(pm));
    }

    @Override
    public MedicationResponse updateScheduleTime(String patientMedicationId, String scheduleId, String timeId,
            ScheduleTimeRequest request) {
        PatientMedication pm = requirePatientMedication(patientMedicationId);

        UserProfile currentProfile = userProfileService.getCurrentUserProfile();
        verifySchedulePermission(currentProfile, pm.getPatientId());

        List<MedicationSchedule> schedules = pm.getMedicationSchedules();
        if (schedules == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found");
        }

        MedicationSchedule schedule = schedules.stream()
                .filter(s -> Objects.equals(s.getId(), scheduleId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found"));

        List<ScheduleTime> scheduleTimes = schedule.getScheduleTimeList();
        if (scheduleTimes == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Time not found");
        }

        ScheduleTime timeToUpdate = scheduleTimes.stream()
                .filter(t -> Objects.equals(t.getId(), timeId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Time not found"));

        patientMedicationMapper.updateModel(timeToUpdate, request);
        return toMedicationResponse(patientMedicationRepository.save(pm));
    }

    @Override
    public MedicationResponse deleteScheduleTime(String patientMedicationId, String scheduleId, String timeId) {
        PatientMedication pm = requirePatientMedication(patientMedicationId);

        UserProfile currentProfile = userProfileService.getCurrentUserProfile();
        verifySchedulePermission(currentProfile, pm.getPatientId());

        List<MedicationSchedule> schedules = pm.getMedicationSchedules();
        if (schedules == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found");
        }

        MedicationSchedule schedule = schedules.stream()
                .filter(s -> Objects.equals(s.getId(), scheduleId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found"));

        List<ScheduleTime> scheduleTimes = schedule.getScheduleTimeList();
        if (scheduleTimes != null) {
            boolean removed = scheduleTimes.removeIf(t -> Objects.equals(t.getId(), timeId));
            if (!removed) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Time not found");
            }
            return toMedicationResponse(patientMedicationRepository.save(pm));
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Time not found");
    }

    private PatientMedication requirePatientMedication(String id) {
        return patientMedicationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient medication not found"));
    }

    private void verifyAccessToPatient(UserProfile currentProfile, String patientId) {
        if (currentProfile.getRole() == Role.ELDERLY) {
            if (!currentProfile.getId().equals(patientId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You cannot access medications of another profile");
            }
            return;
        }

        verifyCaregiverPermission(currentProfile.getId(), patientId);
    }

    private void verifySchedulePermission(UserProfile profile, String patientId) {
        if (profile.getRole() == Role.CAREGIVER) {
            verifyCaregiverPermission(profile.getId(), patientId,
                    ct01.n07.backend.model.enums.PermissionLevel.EDIT_SCHEDULE,
                    ct01.n07.backend.model.enums.PermissionLevel.MANAGE_ALL);
        } else {
            verifyAccessToPatient(profile, patientId);
        }
    }

    private void verifyCaregiverPermission(String caregiverId, String elderlyId) {
        verifyCaregiverPermission(caregiverId, elderlyId, (ct01.n07.backend.model.enums.PermissionLevel[]) null);
    }

    private void verifyCaregiverPermission(String caregiverId, String elderlyId,
            ct01.n07.backend.model.enums.PermissionLevel... requiredLevels) {
        ct01.n07.backend.model.Relationship relationship = relationshipRepository
                .findAllByCaregiverIdAndElderlyIdAndStatus(
                        caregiverId,
                        elderlyId,
                        RelationStatus.ACCEPTED)
                .stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Caregiver does not have an accepted relationship with this patient"));

        if (requiredLevels != null && requiredLevels.length > 0) {
            boolean hasRequiredLevel = false;
            for (ct01.n07.backend.model.enums.PermissionLevel level : requiredLevels) {
                if (relationship.getPermissionLevel() == level) {
                    hasRequiredLevel = true;
                    break;
                }
            }
            if (!hasRequiredLevel) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Caregiver does not have the required permission level to perform this action");
            }
        }
    }

    private void requireRole(UserProfile profile, Role... allowedRoles) {
        for (Role role : allowedRoles) {
            if (profile.getRole() == role) {
                return;
            }
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to perform this action");
    }

    private MedicationSchedule toSimpleDailySchedule(List<String> scheduleTimes) {
        if (scheduleTimes == null || scheduleTimes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one schedule time is required");
        }

        List<ScheduleTime> times = scheduleTimes.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(this::parseScheduleTime)
                .toList();

        if (times.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one valid schedule time is required");
        }

        return MedicationSchedule.builder()
                .scheduleType(ScheduleType.DAILY)
                .isActive(true)
                .scheduleTimeList(times)
                .build();
    }

    private ScheduleTime parseScheduleTime(String value) {
        try {
            LocalTime parsed = LocalTime.parse(value);
            return ScheduleTime.builder().takenTime(parsed).quantity(BigDecimal.ONE).build();
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid schedule time format: " + value + " (expected HH:mm)");
        }
    }

    private MedicationResponse toMedicationResponse(PatientMedication medication) {
        List<String> schedules = medication.getMedicationSchedules() == null
                ? List.of()
                : medication.getMedicationSchedules().stream()
                        .flatMap(schedule -> schedule.getScheduleTimeList() == null
                                ? java.util.stream.Stream.empty()
                                : schedule.getScheduleTimeList().stream())
                        .map(ScheduleTime::getTakenTime)
                        .filter(Objects::nonNull)
                        .map(LocalTime::toString)
                        .toList();

        return MedicationResponse.builder()
                .id(medication.getId())
                .patientId(medication.getPatientId())
                .pillId(medication.getPillId())
                .nickname(medication.getNickname())
                .dosageAmount(medication.getDosageAmount())
                .dosageUnit(medication.getDosageUnit())
                .route(medication.getRoute())
                .mealRelation(medication.getMealRelation())
                .instruction(medication.getInstruction())
                .prescribedBy(medication.getPrescribedBy())
                .purpose(medication.getPurpose())
                .startDate(medication.getStartDate())
                .endDate(medication.getEndDate())
                .isPrn(medication.isPrn())
                .maxPerDay(medication.getMaxPerDay())
                .schedules(schedules)
                .totalQuantity(medication.getTotalQuantity())
                .isActive(medication.isActive())
                .createdAt(medication.getCreatedAt())
                .updatedAt(medication.getUpdatedAt())
                .build();
    }
}
