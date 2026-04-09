package ct01.n07.backend.service;

import ct01.n07.backend.dto.patientMedication.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PatientMedicationService {
    MedicationResponse createMedication(MedicationCreateRequest request);

    Page<MedicationResponse> getMedications(String patientId, Boolean isActive, Pageable pageable);

    MedicationResponse updateMedication(String id, MedicationUpdateRequest request);

    MedicationResponse getPatientMedicationById(String id);

    void deletePatientMedication(String id);

    // Scheduling management
    MedicationResponse addMedicationSchedule(ScheduleRequest scheduleRequest, String patientMedicationId);

    MedicationResponse updateMedicationSchedule(String patientMedicationId, String scheduleId,
            ScheduleRequest request);

    MedicationResponse deleteMedicationSchedule(String patientMedicationId, String scheduleId);

    MedicationResponse addScheduleTime(String patientMedicationId, String scheduleId, ScheduleTimeRequest request);

    MedicationResponse updateScheduleTime(String patientMedicationId, String scheduleId, String timeId,
            ScheduleTimeRequest request);

    MedicationResponse deleteScheduleTime(String patientMedicationId, String scheduleId, String timeId);
}
