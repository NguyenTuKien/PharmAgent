package ct01.n07.backend.service.impl;

import ct01.n07.backend.dto.medication.MedDoseRequest;
import ct01.n07.backend.dto.medication.MedScheduleRequest;
import ct01.n07.backend.dto.medication.MedicationResponse;
import ct01.n07.backend.mapper.MedicationMapper;
import ct01.n07.backend.model.MedDose;
import ct01.n07.backend.model.MedSchedule;
import ct01.n07.backend.model.Medication;
import ct01.n07.backend.model.UserProfile;
import ct01.n07.backend.repository.MedicationRepository;
import ct01.n07.backend.security.MedicationPermissionValidator;
import ct01.n07.backend.security.ProfileAccessContext;
import ct01.n07.backend.service.EventDoseSyncService;
import ct01.n07.backend.service.MedicationCoreService;
import ct01.n07.backend.service.MedicationScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MedicationScheduleServiceImpl implements MedicationScheduleService {

    private final MedicationRepository medicationRepository;
    private final MedicationCoreService medicationCoreService;
    private final ProfileAccessContext profileAccessContext;
    private final MedicationMapper medicationMapper;
    private final MedicationPermissionValidator permissionValidator;
    private final EventDoseSyncService eventDoseSyncService;

    @Override
    public MedicationResponse addMedicationSchedule(MedScheduleRequest scheduleRequest, String medicationId) {
        Medication pm = medicationCoreService.requireMedication(medicationId);

        UserProfile currentProfile = profileAccessContext.getCurrentUserProfile();
        permissionValidator.verifySchedulePermission(currentProfile.getRole(), currentProfile.getId(), pm.getPatientId());

        if (pm.getMedicationSchedules() == null) {
            pm.setMedicationSchedules(new ArrayList<>());
        }

        MedSchedule newSchedule = medicationMapper.toModel(scheduleRequest);
        pm.getMedicationSchedules().add(newSchedule);
        pm.setActive(true);
        Medication saved = medicationRepository.save(pm);

        // Create dose events for new schedule
        if (newSchedule.getScheduleTimeList() != null) {
            for (MedDose time : newSchedule.getScheduleTimeList()) {
                eventDoseSyncService.createDoseEvent(saved, newSchedule, time);
            }
        }

        return medicationCoreService.toMedicationResponse(saved);
    }

    @Override
    public MedicationResponse updateMedicationSchedule(String medicationId, String scheduleId, MedScheduleRequest request) {
        Medication pm = medicationCoreService.requireMedication(medicationId);

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

        // Re-sync dose events
        eventDoseSyncService.deletePendingByScheduleId(scheduleId);
        
        MedSchedule updatedSchedule = savedPm.getMedicationSchedules().stream()
                .filter(s -> Objects.equals(s.getId(), scheduleId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Schedule not found after save"));
        
        if (updatedSchedule.getScheduleTimeList() != null) {
            for (MedDose time : updatedSchedule.getScheduleTimeList()) {
                if (!eventDoseSyncService.hasDoseEventForMedDose(time.getId())) {
                    eventDoseSyncService.createDoseEvent(savedPm, updatedSchedule, time);
                }
            }
        }
        return medicationCoreService.toMedicationResponse(savedPm);
    }

    @Override
    public MedicationResponse deleteMedicationSchedule(String medicationId, String scheduleId) {
        Medication pm = medicationCoreService.requireMedication(medicationId);

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
        eventDoseSyncService.deleteByScheduleId(scheduleId);
        return medicationCoreService.toMedicationResponse(medicationRepository.save(pm));
    }

    @Override
    public MedicationResponse addScheduleTime(String medicationId, String scheduleId, MedDoseRequest request) {
        Medication pm = medicationCoreService.requireMedication(medicationId);

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

        eventDoseSyncService.createDoseEvent(saved, schedule, newTime);

        return medicationCoreService.toMedicationResponse(saved);
    }

    @Override
    public MedicationResponse updateScheduleTime(String medicationId, String scheduleId, String timeId, MedDoseRequest request) {
        Medication pm = medicationCoreService.requireMedication(medicationId);

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

        eventDoseSyncService.syncDoseEventForTimeUpdate(saved, schedule, timeToUpdate);

        return medicationCoreService.toMedicationResponse(saved);
    }

    @Override
    public MedicationResponse deleteScheduleTime(String medicationId, String scheduleId, String timeId) {
        Medication pm = medicationCoreService.requireMedication(medicationId);

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
            eventDoseSyncService.deleteByMedDoseId(timeId);
            return medicationCoreService.toMedicationResponse(medicationRepository.save(pm));
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Time not found");
    }
}
