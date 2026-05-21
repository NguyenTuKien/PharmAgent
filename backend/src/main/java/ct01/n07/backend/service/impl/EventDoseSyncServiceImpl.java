package ct01.n07.backend.service.impl;

import ct01.n07.backend.model.EventDose;
import ct01.n07.backend.model.MedDose;
import ct01.n07.backend.model.MedSchedule;
import ct01.n07.backend.model.Medication;
import ct01.n07.backend.model.enums.DoseStatus;
import ct01.n07.backend.model.enums.ScheduleType;
import ct01.n07.backend.repository.EventDoseRepository;
import ct01.n07.backend.service.EventDoseSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class EventDoseSyncServiceImpl implements EventDoseSyncService {

    private static final int DEFAULT_GENERATION_DAYS = 30;
    private static final int MAX_GENERATION_DAYS = 90;

    private final EventDoseRepository eventDoseRepository;

    @Override
    public void createDoseEvent(Medication pm, MedSchedule schedule, MedDose time) {
        if (time == null || time.getTakenTime() == null || shouldSkipAutomaticEvents(schedule)) {
            return;
        }

        for (LocalDate date : resolveScheduledDates(pm, schedule)) {
            EventDose eventDose = EventDose.builder()
                    .medicationId(pm.getId())
                    .scheduleId(schedule.getId())
                    .medDoseId(time.getId())
                    .scheduledAt(date.atTime(time.getTakenTime()))
                    .status(DoseStatus.PENDING)
                    .build();

            eventDoseRepository.save(eventDose);
        }
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
        deletePendingByMedDoseId(timeToUpdate.getId());
        createDoseEvent(pm, schedule, timeToUpdate);
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
    public void deletePendingByMedDoseId(String medDoseId) {
        eventDoseRepository.deleteByMedDoseIdAndStatus(medDoseId, DoseStatus.PENDING);
    }

    @Override
    public void deletePendingByScheduleId(String scheduleId) {
        eventDoseRepository.deleteByScheduleIdAndStatus(scheduleId, DoseStatus.PENDING);
    }

    @Override
    public boolean hasDoseEventForMedDose(String medDoseId) {
        return eventDoseRepository.existsByMedDoseId(medDoseId);
    }

    private List<LocalDate> resolveScheduledDates(Medication medication, MedSchedule schedule) {
        LocalDate startDate = firstNonNull(schedule.getStartDate(), medication.getStartDate(), LocalDate.now());
        LocalDate requestedEndDate = firstNonNull(schedule.getEndDate(), medication.getEndDate(),
                startDate.plusDays(DEFAULT_GENERATION_DAYS));
        LocalDate cappedEndDate = requestedEndDate.isAfter(startDate.plusDays(MAX_GENERATION_DAYS))
                ? startDate.plusDays(MAX_GENERATION_DAYS)
                : requestedEndDate;

        if (cappedEndDate.isBefore(startDate)) {
            return List.of();
        }

        return startDate.datesUntil(cappedEndDate.plusDays(1))
                .filter(date -> matchesScheduleDate(date, startDate, schedule))
                .toList();
    }

    private boolean matchesScheduleDate(LocalDate date, LocalDate startDate, MedSchedule schedule) {
        ScheduleType type = schedule.getScheduleType() == null ? ScheduleType.DAILY : schedule.getScheduleType();
        int interval = schedule.getFrequencyInterval() == null || schedule.getFrequencyInterval() < 1
                ? 1
                : schedule.getFrequencyInterval();

        return switch (type) {
            case DAILY, INTERVAL, CUSTOM -> ChronoUnit.DAYS.between(startDate, date) % interval == 0;
            case WEEKLY -> isWeeklyMatch(date, startDate, schedule.getDaysOfWeek(), interval);
            case MONTHLY -> isMonthlyMatch(date, startDate, interval);
            case PRN, AS_NEEDED -> false;
        };
    }

    private boolean isWeeklyMatch(LocalDate date, LocalDate startDate, List<String> daysOfWeek, int interval) {
        if (ChronoUnit.WEEKS.between(startDate, date) % interval != 0) {
            return false;
        }

        if (daysOfWeek == null || daysOfWeek.isEmpty()) {
            return date.getDayOfWeek() == startDate.getDayOfWeek();
        }

        return daysOfWeek.stream()
                .map(this::parseDayOfWeek)
                .anyMatch(day -> day == date.getDayOfWeek());
    }

    private boolean isMonthlyMatch(LocalDate date, LocalDate startDate, int interval) {
        long months = ChronoUnit.MONTHS.between(startDate.withDayOfMonth(1), date.withDayOfMonth(1));
        return months % interval == 0 && date.getDayOfMonth() == startDate.getDayOfMonth();
    }

    private DayOfWeek parseDayOfWeek(String rawValue) {
        if (rawValue == null) {
            return null;
        }

        return switch (rawValue.trim().toUpperCase(Locale.ROOT)) {
            case "MON", "MONDAY", "T2", "THU_2" -> DayOfWeek.MONDAY;
            case "TUE", "TUESDAY", "T3", "THU_3" -> DayOfWeek.TUESDAY;
            case "WED", "WEDNESDAY", "T4", "THU_4" -> DayOfWeek.WEDNESDAY;
            case "THU", "THURSDAY", "T5", "THU_5" -> DayOfWeek.THURSDAY;
            case "FRI", "FRIDAY", "T6", "THU_6" -> DayOfWeek.FRIDAY;
            case "SAT", "SATURDAY", "T7", "THU_7" -> DayOfWeek.SATURDAY;
            case "SUN", "SUNDAY", "CN", "CHU_NHAT" -> DayOfWeek.SUNDAY;
            default -> null;
        };
    }

    private boolean shouldSkipAutomaticEvents(MedSchedule schedule) {
        ScheduleType type = schedule.getScheduleType();
        return type == ScheduleType.PRN || type == ScheduleType.AS_NEEDED;
    }

    @SafeVarargs
    private final <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}

