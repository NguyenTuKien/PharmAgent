package ct01.n07.backend.service.impl;

import ct01.n07.backend.dto.patientMedication.*;
import ct01.n07.backend.mapper.PatientMedicationMapper;
import ct01.n07.backend.model.MedicationSchedule;
import ct01.n07.backend.model.PatientMedication;
import ct01.n07.backend.model.ScheduleTime;
import ct01.n07.backend.model.UserProfile;
import ct01.n07.backend.model.enums.Role;
import ct01.n07.backend.repository.PatientMedicationRepository;
import ct01.n07.backend.repository.PillRepository;
import ct01.n07.backend.repository.UserProfileRepository;
import ct01.n07.backend.service.MedicationPermissionValidator;
import ct01.n07.backend.service.DoseEventSyncService;
import ct01.n07.backend.service.PatientMedicationService;
import ct01.n07.backend.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PatientMedicationServiceImpl implements PatientMedicationService {
    private final PatientMedicationRepository patientMedicationRepository;
    private final PillRepository pillRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserProfileService userProfileService;
    private final PatientMedicationMapper patientMedicationMapper;
    private final MedicationPermissionValidator permissionValidator;
    private final DoseEventSyncService doseEventSyncService;

    @Override
    public MedicationResponse createMedication(MedicationCreateRequest request) {
        UserProfile currentProfile = userProfileService.getCurrentUserProfile();
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
                .totalQuantity(request.getTotalQuantity())
                .isActive(true)
                .medicationSchedules(patientMedicationMapper.toModels(request.getSchedules()))
                .build();

        PatientMedication savedPm = patientMedicationRepository.save(medication);
        doseEventSyncService.syncDoseEvents(savedPm);
        return toMedicationResponse(savedPm);
    }

    @Override
    public Page<MedicationResponse> getMedications(String patientId, Boolean isActive, Pageable pageable) {
        UserProfile currentProfile = userProfileService.getCurrentUserProfile();
        permissionValidator.requireRole(currentProfile.getRole(), Role.CAREGIVER, Role.ELDERLY);
        permissionValidator.verifyAccessToPatient(currentProfile.getRole(), currentProfile.getId(), patientId);

        Page<PatientMedication> page = isActive == null
                ? patientMedicationRepository.findByPatientId(patientId, pageable)
                : patientMedicationRepository.findByPatientIdAndIsActive(patientId, isActive, pageable);

        return page.map(this::toMedicationResponse);
    }

    @Override
    public MedicationResponse updateMedication(String id, MedicationUpdateRequest request) {
        UserProfile currentProfile = userProfileService.getCurrentUserProfile();
        permissionValidator.requireRole(currentProfile.getRole(), Role.CAREGIVER, Role.ELDERLY);

        PatientMedication medication = requirePatientMedication(id);

        permissionValidator.verifySchedulePermission(currentProfile.getRole(), currentProfile.getId(), medication.getPatientId());

        if (request.getPillId() != null) {
            if (!pillRepository.existsById(request.getPillId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pill not found");
            }
            medication.setPillId(request.getPillId());
        }

        if (request.getSchedules() != null) {
            medication.setMedicationSchedules(patientMedicationMapper.toModels(request.getSchedules()));
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
            
        PatientMedication savedPm = patientMedicationRepository.save(medication);
        if (request.getSchedules() != null) {
            doseEventSyncService.syncDoseEvents(savedPm);
        }
        return toMedicationResponse(savedPm);
    }

    @Override
    public MedicationResponse getPatientMedicationById(String id) {
        return toMedicationResponse(requirePatientMedication(id));
    }

    @Override
    public MedicationResponse addMedicationSchedule(ScheduleRequest scheduleRequest, String patientMedicationId) {
        PatientMedication patientMedication = requirePatientMedication(patientMedicationId);

        UserProfile currentProfile = userProfileService.getCurrentUserProfile();
        permissionValidator.verifySchedulePermission(currentProfile.getRole(), currentProfile.getId(), patientMedication.getPatientId());

        if (patientMedication.getMedicationSchedules() == null) {
            patientMedication.setMedicationSchedules(new ArrayList<>());
        }

        MedicationSchedule newSchedule = patientMedicationMapper.toModel(scheduleRequest);
        patientMedication.getMedicationSchedules().add(newSchedule);
        patientMedication.setActive(true);
        PatientMedication saved = patientMedicationRepository.save(patientMedication);
        
        // Tạo dose events cho schedule mới
        if (newSchedule.getScheduleTimeList() != null) {
            for (ScheduleTime time : newSchedule.getScheduleTimeList()) {
                doseEventSyncService.createDoseEvent(saved, newSchedule, time);
            }
        }
        
        return toMedicationResponse(saved);
    }

    @Override
    public void deletePatientMedication(String id) {
        PatientMedication pm = requirePatientMedication(id);
        UserProfile currentProfile = userProfileService.getCurrentUserProfile();
        permissionValidator.verifySchedulePermission(currentProfile.getRole(), currentProfile.getId(), pm.getPatientId());
        
        patientMedicationRepository.deleteById(id);
        doseEventSyncService.deleteByPatientMedicationId(id);
    }

    @Override
    public MedicationResponse updateMedicationSchedule(String patientMedicationId, String scheduleId,
                                                      ScheduleRequest request) {
        PatientMedication pm = requirePatientMedication(patientMedicationId);

        UserProfile currentProfile = userProfileService.getCurrentUserProfile();
        permissionValidator.verifySchedulePermission(currentProfile.getRole(), currentProfile.getId(), pm.getPatientId());

        List<MedicationSchedule> schedules = pm.getMedicationSchedules();
        if (schedules == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found");
        }

        MedicationSchedule scheduleToUpdate = schedules.stream()
                .filter(s -> Objects.equals(s.getId(), scheduleId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found"));

        patientMedicationMapper.updateModel(scheduleToUpdate, request);
        PatientMedication savedPm = patientMedicationRepository.save(pm);
        // Re-sync dose events for the updated schedule:
        // Only delete PENDING events to preserve user-recorded data (TAKEN/MISSED/SKIPPED)
        doseEventSyncService.deletePendingByScheduleId(scheduleId);
        MedicationSchedule updatedSchedule = savedPm.getMedicationSchedules().stream()
                .filter(s -> Objects.equals(s.getId(), scheduleId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Schedule not found after save"));
        if (updatedSchedule.getScheduleTimeList() != null) {
            for (ScheduleTime time : updatedSchedule.getScheduleTimeList()) {
                // Only create a new event if no existing event references this scheduleTimeId
                if (!doseEventSyncService.hasDoseEventForScheduleTime(time.getId())) {
                    doseEventSyncService.createDoseEvent(savedPm, updatedSchedule, time);
                }
            }
        }
        return toMedicationResponse(savedPm);
    }

    @Override
    public MedicationResponse deleteMedicationSchedule(String patientMedicationId, String scheduleId) {
        PatientMedication pm = requirePatientMedication(patientMedicationId);

        UserProfile currentProfile = userProfileService.getCurrentUserProfile();
        permissionValidator.verifySchedulePermission(currentProfile.getRole(), currentProfile.getId(), pm.getPatientId());

        List<MedicationSchedule> schedules = pm.getMedicationSchedules();
        if (schedules == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found");
        }
        boolean removed = schedules.removeIf(s -> Objects.equals(s.getId(), scheduleId));
        if (!removed) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found");
        }
        doseEventSyncService.deleteByScheduleId(scheduleId);
        return toMedicationResponse(patientMedicationRepository.save(pm));
    }

    @Override
    public MedicationResponse addScheduleTime(String patientMedicationId, String scheduleId,
            ScheduleTimeRequest request) {
        PatientMedication pm = requirePatientMedication(patientMedicationId);

        UserProfile currentProfile = userProfileService.getCurrentUserProfile();
        permissionValidator.verifySchedulePermission(currentProfile.getRole(), currentProfile.getId(), pm.getPatientId());

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

        ScheduleTime newTime = patientMedicationMapper.toModel(request);
        schedule.getScheduleTimeList().add(newTime);
        PatientMedication saved = patientMedicationRepository.save(pm);
        
        doseEventSyncService.createDoseEvent(saved, schedule, newTime);
        
        return toMedicationResponse(saved);
    }

    @Override
    public MedicationResponse updateScheduleTime(String patientMedicationId, String scheduleId, String timeId,
            ScheduleTimeRequest request) {
        PatientMedication pm = requirePatientMedication(patientMedicationId);

        UserProfile currentProfile = userProfileService.getCurrentUserProfile();
        permissionValidator.verifySchedulePermission(currentProfile.getRole(), currentProfile.getId(), pm.getPatientId());

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
        PatientMedication saved = patientMedicationRepository.save(pm);
        
        // Cập nhật DoseEvent nếu có
        doseEventSyncService.syncDoseEventForTimeUpdate(pm, schedule, timeToUpdate);
        
        return toMedicationResponse(saved);
    }

    @Override
    public MedicationResponse deleteScheduleTime(String patientMedicationId, String scheduleId, String timeId) {
        PatientMedication pm = requirePatientMedication(patientMedicationId);

        UserProfile currentProfile = userProfileService.getCurrentUserProfile();
        permissionValidator.verifySchedulePermission(currentProfile.getRole(), currentProfile.getId(), pm.getPatientId());

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
            doseEventSyncService.deleteByScheduleTimeId(timeId);
            return toMedicationResponse(patientMedicationRepository.save(pm));
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Time not found");
    }

    private PatientMedication requirePatientMedication(String id) {
        return patientMedicationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient medication not found"));
    }

    private MedicationResponse toMedicationResponse(PatientMedication medication) {
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
                .schedules(patientMedicationMapper.toResponses(medication.getMedicationSchedules()))
                .totalQuantity(medication.getTotalQuantity())
                .isActive(medication.isActive())
                .createdAt(medication.getCreatedAt())
                .updatedAt(medication.getUpdatedAt())
                .build();
    }

    @Override
    public long countDistinctActivePatients() {
        Long count = patientMedicationRepository.countDistinctActivePatients();
        return count != null ? count : 0L;
    }
}
