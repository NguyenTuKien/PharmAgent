package ct01.n07.backend.facade;

import ct01.n07.backend.dto.medication.*;
import ct01.n07.backend.service.MedicationCoreService;
import ct01.n07.backend.service.MedicationScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MedicationFacade {

    private final MedicationCoreService medicationCoreService;
    private final MedicationScheduleService medicationScheduleService;

    public MedicationResponse createMedication(MedicationCreateRequest request) {
        return medicationCoreService.createMedication(request);
    }

    public Page<MedicationResponse> getMedications(String patientId, Boolean isActive, Pageable pageable) {
        return medicationCoreService.getMedications(patientId, isActive, pageable);
    }

    public MedicationResponse updateMedication(String id, MedicationUpdateRequest request) {
        return medicationCoreService.updateMedication(id, request);
    }

    public MedicationResponse getMedicationById(String id) {
        return medicationCoreService.getMedicationById(id);
    }

    public void deleteMedication(String id) {
        medicationCoreService.deleteMedication(id);
    }

    public MedicationResponse addMedicationSchedule(MedScheduleRequest scheduleRequest, String medicationId) {
        return medicationScheduleService.addMedicationSchedule(scheduleRequest, medicationId);
    }

    public MedicationResponse updateMedicationSchedule(String medicationId, String scheduleId, MedScheduleRequest request) {
        return medicationScheduleService.updateMedicationSchedule(medicationId, scheduleId, request);
    }

    public MedicationResponse deleteMedicationSchedule(String medicationId, String scheduleId) {
        return medicationScheduleService.deleteMedicationSchedule(medicationId, scheduleId);
    }

    public MedicationResponse addScheduleTime(String medicationId, String scheduleId, MedDoseRequest request) {
        return medicationScheduleService.addScheduleTime(medicationId, scheduleId, request);
    }

    public MedicationResponse updateScheduleTime(String medicationId, String scheduleId, String timeId, MedDoseRequest request) {
        return medicationScheduleService.updateScheduleTime(medicationId, scheduleId, timeId, request);
    }

    public MedicationResponse deleteScheduleTime(String medicationId, String scheduleId, String timeId) {
        return medicationScheduleService.deleteScheduleTime(medicationId, scheduleId, timeId);
    }
}
