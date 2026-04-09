package ct01.n07.backend.service.impl;

import ct01.n07.backend.model.EventDose;
import ct01.n07.backend.model.MedDose;
import ct01.n07.backend.model.MedSchedule;
import ct01.n07.backend.model.Medication;
import ct01.n07.backend.model.enums.DoseStatus;
import ct01.n07.backend.repository.EventDoseRepository;
import ct01.n07.backend.service.EventDoseSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class EventDoseSyncServiceImpl implements EventDoseSyncService {

    private final EventDoseRepository eventDoseRepository;

    @Override
    public void createDoseEvent(Medication pm, MedSchedule schedule, MedDose time) {
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

    @Override
    public void syncDoseEvents(Medication pm) {
        // Xóa tất cả dose events cũ của medication này và tạo lại
        eventDoseRepository.deleteByMedicationId(pm.getId());
        if (pm.getMedicationSchedules() != null) {
            for (MedSchedule schedule : pm.getMedicationSchedules()) {
                if (schedule.getScheduleTimeList() != null) {
                    for (MedDose time : schedule.getScheduleTimeList()) {
                        createDoseEvent(pm, schedule, time);
                    }
                }
            }
        }
    }

    @Override
    public void syncDoseEventForTimeUpdate(Medication pm, MedSchedule schedule, MedDose timeToUpdate) {
        eventDoseRepository.findByMedDoseId(timeToUpdate.getId()).ifPresent(event -> {
            LocalDate date = schedule.getStartDate() != null ? schedule.getStartDate()
                    : (pm.getStartDate() != null ? pm.getStartDate() : LocalDate.now());
            event.setScheduledAt(date.atTime(timeToUpdate.getTakenTime()));
            eventDoseRepository.save(event);
        });
    }

    @Override
    public void deleteByMedicationId(String medicationId) {
        eventDoseRepository.deleteByMedicationId(medicationId);
    }

    @Override
    public void deleteByScheduleId(String scheduleId) {
        eventDoseRepository.deleteByScheduleId(scheduleId);
    }

    @Override
    public void deleteByMedDoseId(String medDoseId) {
        eventDoseRepository.deleteByMedDoseId(medDoseId);
    }

    @Override
    public void deletePendingByScheduleId(String scheduleId) {
        eventDoseRepository.deleteByScheduleIdAndStatus(scheduleId, DoseStatus.PENDING);
    }

    @Override
    public boolean hasDoseEventForMedDose(String medDoseId) {
        return eventDoseRepository.findByMedDoseId(medDoseId).isPresent();
    }
}

