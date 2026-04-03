package ct01.web.backend.service;

import ct01.web.backend.dto.doseEvent.MedicationScheduleRequest;
import ct01.web.backend.dto.doseEvent.PatientMedicationRequest;
import ct01.web.backend.dto.doseEvent.ScheduleTimeRequest;
import ct01.web.backend.model.PatientMedication;
import org.jspecify.annotations.Nullable;

import java.util.List;

public interface PatientMedicationService {
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
