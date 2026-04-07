package ct01.n07.backend.service;

import ct01.n07.backend.dto.patientMedication.MedicationCreateRequest;
import ct01.n07.backend.dto.patientMedication.MedicationResponse;
import ct01.n07.backend.dto.patientMedication.MedicationScheduleRequest;
import ct01.n07.backend.dto.patientMedication.MedicationUpdateRequest;
import ct01.n07.backend.dto.patientMedication.ScheduleTimeRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.domain.Page;

public interface PatientMedicationService {
    MedicationResponse createMedication(MedicationCreateRequest request);

    Page<MedicationResponse> getMedications(String patientId, Boolean isActive, Pageable pageable);

    MedicationResponse updateMedication(String id, MedicationUpdateRequest request);

    MedicationResponse getPatientMedicationById(String id);

    void deletePatientMedication(String id);

    // Scheduling management
    MedicationResponse addMedicationSchedule(MedicationScheduleRequest medicationScheduleRequest);

    MedicationResponse updateMedicationSchedule(String patientMedicationId, String scheduleId,
            MedicationScheduleRequest request);

    MedicationResponse deleteMedicationSchedule(String patientMedicationId, String scheduleId);

    MedicationResponse addScheduleTime(String patientMedicationId, String scheduleId, ScheduleTimeRequest request);

    MedicationResponse updateScheduleTime(String patientMedicationId, String scheduleId, String timeId,
            ScheduleTimeRequest request);

    MedicationResponse deleteScheduleTime(String patientMedicationId, String scheduleId, String timeId);
}
