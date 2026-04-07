package ct01.n07.backend.service;

import ct01.n07.backend.dto.doseEvent.MedicationScheduleRequest;
import ct01.n07.backend.dto.doseEvent.MedicationCreateRequest;
import ct01.n07.backend.dto.doseEvent.MedicationResponse;
import ct01.n07.backend.dto.doseEvent.MedicationUpdateRequest;
import ct01.n07.backend.dto.doseEvent.PatientMedicationRequest;
import ct01.n07.backend.dto.doseEvent.ScheduleTimeRequest;
import ct01.n07.backend.model.PatientMedication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.jspecify.annotations.Nullable;

import java.util.List;

public interface PatientMedicationService {
    MedicationResponse createMedication(MedicationCreateRequest request);

    Page<MedicationResponse> getMedications(String patientId, Boolean isActive, Pageable pageable);

    MedicationResponse updateMedication(String id, MedicationUpdateRequest request);

    @Nullable List<PatientMedication> getAllPatientMedications();

    @Nullable List<PatientMedication> getPatientMedicationsByPatientId(String patientId);

    @Nullable PatientMedication createPatientMedication(PatientMedicationRequest patientMedicationRequest);

    @Nullable PatientMedication updatePatientMedication(String id, PatientMedicationRequest patientMedicationRequest);

    @Nullable PatientMedication addMedicationSchedule(MedicationScheduleRequest medicationScheduleRequest);

    void deletePatientMedication(String id);

    @Nullable PatientMedication updateMedicationSchedule(String patientMedicationId, String scheduleId, MedicationScheduleRequest request);

    @Nullable PatientMedication deleteMedicationSchedule(String patientMedicationId, String scheduleId);

    @Nullable PatientMedication addScheduleTime(String patientMedicationId, String scheduleId, ScheduleTimeRequest request);

    @Nullable PatientMedication updateScheduleTime(String patientMedicationId, String scheduleId, String timeId, ScheduleTimeRequest request);

    @Nullable PatientMedication deleteScheduleTime(String patientMedicationId, String scheduleId, String timeId);

    @Nullable PatientMedication getPatientMedicationById(String id);
}
