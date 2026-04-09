package ct01.n07.backend.service;

import ct01.n07.backend.dto.medication.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MedicationService {
    MedicationResponse createMedication(MedicationCreateRequest request);

    Page<MedicationResponse> getMedications(String patientId, Boolean isActive, Pageable pageable);

    MedicationResponse updateMedication(String id, MedicationUpdateRequest request);

    MedicationResponse getMedicationById(String id);

    void deleteMedication(String id);

    // Scheduling management
    MedicationResponse addMedicationSchedule(MedScheduleRequest scheduleRequest, String medicationId);

    MedicationResponse updateMedicationSchedule(String medicationId, String scheduleId,
            MedScheduleRequest request);

    MedicationResponse deleteMedicationSchedule(String medicationId, String scheduleId);

    MedicationResponse addScheduleTime(String medicationId, String scheduleId, MedDoseRequest request);

    MedicationResponse updateScheduleTime(String medicationId, String scheduleId, String timeId,
            MedDoseRequest request);

    MedicationResponse deleteScheduleTime(String medicationId, String scheduleId, String timeId);

    // Stats management
    Long countDistinctActivePatients();
}

