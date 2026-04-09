package ct01.n07.backend.service.impl;

import ct01.n07.backend.dto.medication.*;
import ct01.n07.backend.mapper.MedicationMapper;
import ct01.n07.backend.model.EventDose;
import ct01.n07.backend.model.MedDose;
import ct01.n07.backend.model.MedSchedule;
import ct01.n07.backend.model.Medication;
import ct01.n07.backend.model.UserProfile;
import ct01.n07.backend.model.enums.DoseStatus;
import ct01.n07.backend.model.enums.Role;
import ct01.n07.backend.repository.EventDoseRepository;
import ct01.n07.backend.repository.MedicationRepository;
import ct01.n07.backend.repository.PillRepository;
import ct01.n07.backend.repository.UserProfileRepository;
import ct01.n07.backend.security.MedicationPermissionValidator;
import ct01.n07.backend.security.ProfileAccessContext;
import ct01.n07.backend.service.MedicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MedicationServiceImpl implements MedicationService {
    private final EventDoseRepository eventDoseRepository;
    private final MedicationRepository medicationRepository;
    private final PillRepository pillRepository;
    private final UserProfileRepository userProfileRepository;
    private final ProfileAccessContext profileAccessContext;
    private final MedicationMapper medicationMapper;
    private final MedicationPermissionValidator permissionValidator;

    @Override
    public MedicationResponse createMedication(MedicationCreateRequest request) {
        UserProfile currentProfile = profileAccessContext.getCurrentUserProfile();
        permissionValidator.requireRole(currentProfile.getRole(), Role.CAREGIVER, Role.ELDERLY);

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

        permissionValidator.verifySchedulePermission(currentProfile.getRole(), currentProfile.getId(), request.getPatientId());

        Medication medication = Medication.builder()
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
                .totalQuantity(request.getTotalQuantity())
                .isActive(true)
                .medicationSchedules(medicationMapper.toModels(request.getSchedules()))
                .build();

        Medication savedPm = medicationRepository.save(medication);
        syncDoseEvents(savedPm);
        return toMedicationResponse(savedPm);
    }

    @Override
    public Page<MedicationResponse> getMedications(String patientId, Boolean isActive, Pageable pageable) {
        UserProfile currentProfile = profileAccessContext.getCurrentUserProfile();
        permissionValidator.requireRole(currentProfile.getRole(), Role.CAREGIVER, Role.ELDERLY);
        permissionValidator.verifyAccessToPatient(currentProfile.getRole(), currentProfile.getId(), patientId);

        Page<Medication> page = isActive == null
                ? medicationRepository.findByPatientId(patientId, pageable)
                : medicationRepository.findByPatientIdAndIsActive(patientId, isActive, pageable);

        return page.map(this::toMedicationResponse);
    }

    @Override
    public MedicationResponse updateMedication(String id, MedicationUpdateRequest request) {
        UserProfile currentProfile = profileAccessContext.getCurrentUserProfile();
        permissionValidator.requireRole(currentProfile.getRole(), Role.CAREGIVER, Role.ELDERLY);

        Medication medication = requirePatientMedication(id);

        permissionValidator.verifySchedulePermission(currentProfile.getRole(), currentProfile.getId(), medication.getPatientId());

        if (request.getPillId() != null) {
            if (!pillRepository.existsById(request.getPillId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pill not found");
            }
            medication.setPillId(request.getPillId());
        }

        if (request.getSchedules() != null) {
            medication.setMedicationSchedules(medicationMapper.toModels(request.getSchedules()));
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
            
        Medication savedPm = medicationRepository.save(medication);
        if (request.getSchedules() != null) {
            syncDoseEvents(savedPm);
        }
        return toMedicationResponse(savedPm);
    }

    @Override
    public MedicationResponse getMedicationById(String id) {
        return toMedicationResponse(requirePatientMedication(id));
    }

    @Override
    public MedicationResponse addMedicationSchedule(MedScheduleRequest scheduleRequest, String medicationId) {
        Medication patientMedication = requirePatientMedication(medicationId);

        UserProfile currentProfile = profileAccessContext.getCurrentUserProfile();
        permissionValidator.verifySchedulePermission(currentProfile.getRole(), currentProfile.getId(), patientMedication.getPatientId());

        if (patientMedication.getMedicationSchedules() == null) {
            patientMedication.setMedicationSchedules(new ArrayList<>());
        }

        MedSchedule newSchedule = medicationMapper.toModel(scheduleRequest);
        patientMedication.getMedicationSchedules().add(newSchedule);
        patientMedication.setActive(true);
        Medication saved = medicationRepository.save(patientMedication);
        
        // Tạo dose events cho schedule mới
        if (newSchedule.getScheduleTimeList() != null) {
            for (MedDose time : newSchedule.getScheduleTimeList()) {
                createDoseEvent(saved, newSchedule, time);
            }
        }
        
        return toMedicationResponse(saved);
    }

    @Override
    public void deleteMedication(String id) {
        Medication pm = requirePatientMedication(id);
        UserProfile currentProfile = profileAccessContext.getCurrentUserProfile();
        permissionValidator.verifySchedulePermission(currentProfile.getRole(), currentProfile.getId(), pm.getPatientId());
        
        medicationRepository.deleteById(id);
        eventDoseRepository.deleteByMedicationId(id);
    }

    @Override
    public MedicationResponse updateMedicationSchedule(String medicationId, String scheduleId,
                                                      MedScheduleRequest request) {
        Medication pm = requirePatientMedication(medicationId);

        UserProfile currentProfile = profileAccessContext.getCurrentUserProfile();
        permissionValidator.verifySchedulePermission(currentProfile.getRole(), currentProfile.getId(), pm.getPatientId());

        List<MedSchedule> schedules = pm.getMedicationSchedules();
        if (schedules == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found");
        }

        MedSchedule scheduleToUpdate = schedules.stream()
                .filter(s -> Objects.equals(s.getId(), scheduleId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found"));

        medicationMapper.updateModel(scheduleToUpdate, request);
        Medication savedPm = medicationRepository.save(pm);
        // Re-sync dose events for the updated schedule:
        // Only delete PENDING events to preserve user-recorded data (TAKEN/MISSED/SKIPPED)
        eventDoseRepository.deleteByScheduleIdAndStatus(scheduleId, DoseStatus.PENDING);
        MedSchedule updatedSchedule = savedPm.getMedicationSchedules().stream()
                .filter(s -> Objects.equals(s.getId(), scheduleId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Schedule not found after save"));
        if (updatedSchedule.getScheduleTimeList() != null) {
            for (MedDose time : updatedSchedule.getScheduleTimeList()) {
                // Only create a new stats if no existing stats references this medDoseId
                if (eventDoseRepository.findByMedDoseId(time.getId()).isEmpty()) {
                    createDoseEvent(savedPm, updatedSchedule, time);
                }
            }
        }
        return toMedicationResponse(savedPm);
    }

    @Override
    public MedicationResponse deleteMedicationSchedule(String medicationId, String scheduleId) {
        Medication pm = requirePatientMedication(medicationId);

        UserProfile currentProfile = profileAccessContext.getCurrentUserProfile();
        permissionValidator.verifySchedulePermission(currentProfile.getRole(), currentProfile.getId(), pm.getPatientId());

        List<MedSchedule> schedules = pm.getMedicationSchedules();
        if (schedules == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found");
        }
        boolean removed = schedules.removeIf(s -> Objects.equals(s.getId(), scheduleId));
        if (!removed) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found");
        }
        eventDoseRepository.deleteByScheduleId(scheduleId);
        return toMedicationResponse(medicationRepository.save(pm));
    }

    @Override
    public MedicationResponse addScheduleTime(String medicationId, String scheduleId,
            MedDoseRequest request) {
        Medication pm = requirePatientMedication(medicationId);

        UserProfile currentProfile = profileAccessContext.getCurrentUserProfile();
        permissionValidator.verifySchedulePermission(currentProfile.getRole(), currentProfile.getId(), pm.getPatientId());

        List<MedSchedule> schedules = pm.getMedicationSchedules();
        if (schedules == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found");
        }

        MedSchedule schedule = schedules.stream()
                .filter(s -> Objects.equals(s.getId(), scheduleId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found"));

        if (schedule.getScheduleTimeList() == null) {
            schedule.setScheduleTimeList(new ArrayList<>());
        }

        MedDose newTime = medicationMapper.toModel(request);
        schedule.getScheduleTimeList().add(newTime);
        Medication saved = medicationRepository.save(pm);
        
        createDoseEvent(saved, schedule, newTime);
        
        return toMedicationResponse(saved);
    }

    @Override
    public MedicationResponse updateScheduleTime(String medicationId, String scheduleId, String timeId,
            MedDoseRequest request) {
        Medication pm = requirePatientMedication(medicationId);

        UserProfile currentProfile = profileAccessContext.getCurrentUserProfile();
        permissionValidator.verifySchedulePermission(currentProfile.getRole(), currentProfile.getId(), pm.getPatientId());

        List<MedSchedule> schedules = pm.getMedicationSchedules();
        if (schedules == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found");
        }

        MedSchedule schedule = schedules.stream()
                .filter(s -> Objects.equals(s.getId(), scheduleId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found"));

        List<MedDose> scheduleTimes = schedule.getScheduleTimeList();
        if (scheduleTimes == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Time not found");
        }

        MedDose timeToUpdate = scheduleTimes.stream()
                .filter(t -> Objects.equals(t.getId(), timeId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Time not found"));

        medicationMapper.updateModel(timeToUpdate, request);
        Medication saved = medicationRepository.save(pm);
        
        // Cập nhật DoseEvent nếu có
        eventDoseRepository.findByMedDoseId(timeToUpdate.getId()).ifPresent(event -> {
            LocalDate date = schedule.getStartDate() != null ? schedule.getStartDate()
                    : (pm.getStartDate() != null ? pm.getStartDate() : LocalDate.now());
            event.setScheduledAt(date.atTime(timeToUpdate.getTakenTime()));
            eventDoseRepository.save(event);
        });
        
        return toMedicationResponse(saved);
    }

    @Override
    public MedicationResponse deleteScheduleTime(String medicationId, String scheduleId, String timeId) {
        Medication pm = requirePatientMedication(medicationId);

        UserProfile currentProfile = profileAccessContext.getCurrentUserProfile();
        permissionValidator.verifySchedulePermission(currentProfile.getRole(), currentProfile.getId(), pm.getPatientId());

        List<MedSchedule> schedules = pm.getMedicationSchedules();
        if (schedules == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found");
        }

        MedSchedule schedule = schedules.stream()
                .filter(s -> Objects.equals(s.getId(), scheduleId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found"));

        List<MedDose> scheduleTimes = schedule.getScheduleTimeList();
        if (scheduleTimes != null) {
            boolean removed = scheduleTimes.removeIf(t -> Objects.equals(t.getId(), timeId));
            if (!removed) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Time not found");
            }
            eventDoseRepository.deleteByMedDoseId(timeId);
            return toMedicationResponse(medicationRepository.save(pm));
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Time not found");
    }

    private void createDoseEvent(Medication pm, MedSchedule schedule, MedDose time) {
        LocalDate date = schedule.getStartDate() != null ? schedule.getStartDate()
                : (pm.getStartDate() != null ? pm.getStartDate() : LocalDate.now());

        EventDose eventDose = EventDose.builder()
                .medicationId(pm.getId())
                .scheduleId(schedule.getId())
                .medDoseId(time.getId())
                .scheduledAt(date.atTime(time.getTakenTime()))
                .status(DoseStatus.PENDING)
                .build();

        eventDoseRepository.save(eventDose);
    }

    private void syncDoseEvents(Medication pm) {
        eventDoseRepository.deleteByMedicationId(pm.getId());
        if (pm.getMedicationSchedules() == null) {
            return;
        }
        for (MedSchedule schedule : pm.getMedicationSchedules()) {
            if (schedule.getScheduleTimeList() == null) {
                continue;
            }
            for (MedDose time : schedule.getScheduleTimeList()) {
                createDoseEvent(pm, schedule, time);
            }
        }
    }

    private Medication requirePatientMedication(String id) {
        return medicationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Medication not found"));
    }

    private MedicationResponse toMedicationResponse(Medication medication) {
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
                .schedules(medicationMapper.toResponses(medication.getMedicationSchedules()))
                .totalQuantity(medication.getTotalQuantity())
                .isActive(medication.isActive())
                .createdAt(medication.getCreatedAt())
                .updatedAt(medication.getUpdatedAt())
                .build();
    }
}



