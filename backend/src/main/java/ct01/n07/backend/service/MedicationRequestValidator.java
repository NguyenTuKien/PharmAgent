package ct01.n07.backend.service;

import ct01.n07.backend.dto.medication.MedScheduleRequest;
import ct01.n07.backend.dto.medication.MedicationCreateRequest;
import ct01.n07.backend.dto.medication.MedicationUpdateRequest;
import ct01.n07.backend.model.Medication;
import ct01.n07.backend.model.enums.ScheduleType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Component
public class MedicationRequestValidator {

    public void validateCreate(MedicationCreateRequest request) {
        validateDateWindow(request.getStartDate(), request.getEndDate(), "Medication endDate cannot be before startDate");
        validateScheduleList(request.getSchedules(), request.getStartDate(), request.getEndDate(), true);
    }

    public void validateUpdate(Medication currentMedication, MedicationUpdateRequest request) {
        LocalDate startDate = request.getStartDate() != null
                ? request.getStartDate()
                : currentMedication.getStartDate();
        LocalDate endDate = request.getEndDate() != null
                ? request.getEndDate()
                : currentMedication.getEndDate();

        validateDateWindow(startDate, endDate, "Medication endDate cannot be before startDate");

        if (request.getSchedules() != null) {
            validateScheduleList(request.getSchedules(), startDate, endDate, true);
        }
    }

    public void validateSchedule(Medication medication, MedScheduleRequest request) {
        validateScheduleRequest(request, medication.getStartDate(), medication.getEndDate());
    }

    public void validateScheduleList(
            List<MedScheduleRequest> schedules,
            LocalDate medicationStartDate,
            LocalDate medicationEndDate,
            boolean requireAtLeastOne
    ) {
        if (schedules == null || schedules.isEmpty()) {
            if (requireAtLeastOne) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Medication requires at least one schedule");
            }
            return;
        }

        schedules.forEach(schedule -> validateScheduleRequest(schedule, medicationStartDate, medicationEndDate));
    }

    private void validateScheduleRequest(
            MedScheduleRequest schedule,
            LocalDate medicationStartDate,
            LocalDate medicationEndDate
    ) {
        LocalDate scheduleStart = schedule.getStartDate() != null ? schedule.getStartDate() : medicationStartDate;
        LocalDate scheduleEnd = schedule.getEndDate() != null ? schedule.getEndDate() : medicationEndDate;
        validateDateWindow(scheduleStart, scheduleEnd, "Schedule endDate cannot be before startDate");

        if (requiresDoseTimes(schedule)
                && (schedule.getMedDoseRequests() == null || schedule.getMedDoseRequests().isEmpty())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Schedule requires at least one dose time");
        }
    }

    private boolean requiresDoseTimes(MedScheduleRequest schedule) {
        ScheduleType type = schedule.getScheduleType();
        return type != ScheduleType.PRN && type != ScheduleType.AS_NEEDED;
    }

    private void validateDateWindow(LocalDate startDate, LocalDate endDate, String message) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }
}
