package ct01.n07.backend.service;

import ct01.n07.backend.model.DoseEvent;
import ct01.n07.backend.model.MedicationSchedule;
import ct01.n07.backend.model.PatientMedication;
import ct01.n07.backend.model.ScheduleTime;
import ct01.n07.backend.model.enums.DoseStatus;
import ct01.n07.backend.repository.DoseEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DoseEventSyncService {

    private final DoseEventRepository doseEventRepository;

    public void createDoseEvent(PatientMedication pm, MedicationSchedule schedule, ScheduleTime time) {
        LocalDate date = schedule.getStartDate() != null ? schedule.getStartDate()
                : (pm.getStartDate() != null ? pm.getStartDate() : LocalDate.now());

        DoseEvent doseEvent = DoseEvent.builder()
                .patientMedicationId(pm.getId())
                .scheduleId(schedule.getId())
                .scheduleTimeId(time.getId())
                .scheduledAt(date.atTime(time.getTakenTime()))
                .status(DoseStatus.PENDING)
                .build();

        doseEventRepository.save(doseEvent);
    }

    public void syncDoseEvents(PatientMedication pm) {
        // Xóa tất cả dose events cũ của medication này và tạo lại
        doseEventRepository.deleteByPatientMedicationId(pm.getId());
        if (pm.getMedicationSchedules() != null) {
            for (MedicationSchedule schedule : pm.getMedicationSchedules()) {
                if (schedule.getScheduleTimeList() != null) {
                    for (ScheduleTime time : schedule.getScheduleTimeList()) {
                        createDoseEvent(pm, schedule, time);
                    }
                }
            }
        }
    }

    public void syncDoseEventForTimeUpdate(PatientMedication pm, MedicationSchedule schedule, ScheduleTime timeToUpdate) {
        doseEventRepository.findByScheduleTimeId(timeToUpdate.getId()).ifPresent(event -> {
            LocalDate date = schedule.getStartDate() != null ? schedule.getStartDate()
                    : (pm.getStartDate() != null ? pm.getStartDate() : LocalDate.now());
            event.setScheduledAt(date.atTime(timeToUpdate.getTakenTime()));
            doseEventRepository.save(event);
        });
    }

    public void deletePendingByScheduleId(String scheduleId) {
        if (scheduleId == null || scheduleId.isBlank()) return;
        doseEventRepository.deleteByScheduleIdAndStatus(scheduleId, DoseStatus.PENDING);
    }

    public boolean hasDoseEventForScheduleTime(String scheduleTimeId) {
        if (scheduleTimeId == null || scheduleTimeId.isBlank()) return false;
        return doseEventRepository.findByScheduleTimeId(scheduleTimeId).isPresent();
    }

    public void deleteByPatientMedicationId(String pmId) {
        doseEventRepository.deleteByPatientMedicationId(pmId);
    }

    public void deleteByScheduleId(String scheduleId) {
        doseEventRepository.deleteByScheduleId(scheduleId);
    }

    public void deleteByScheduleTimeId(String timeId) {
        doseEventRepository.deleteByScheduleTimeId(timeId);
    }
}
