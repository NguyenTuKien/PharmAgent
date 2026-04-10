package ct01.n07.backend.service;

import ct01.n07.backend.dto.medication.MedDoseRequest;
import ct01.n07.backend.dto.medication.MedScheduleRequest;
import ct01.n07.backend.dto.medication.MedicationResponse;

public interface MedicationScheduleService {
    MedicationResponse addMedicationSchedule(MedScheduleRequest scheduleRequest, String medicationId);

    MedicationResponse updateMedicationSchedule(String medicationId, String scheduleId,
                                               MedScheduleRequest request);

    MedicationResponse deleteMedicationSchedule(String medicationId, String scheduleId);

    MedicationResponse addScheduleTime(String medicationId, String scheduleId, MedDoseRequest request);

    MedicationResponse updateScheduleTime(String medicationId, String scheduleId, String timeId,
                                         MedDoseRequest request);

    MedicationResponse deleteScheduleTime(String medicationId, String scheduleId, String timeId);
}
