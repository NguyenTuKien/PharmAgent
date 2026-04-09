package ct01.n07.backend.controller.caregiver;

import ct01.n07.backend.dto.patientMedication.*;
import ct01.n07.backend.service.PatientMedicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/caregiver/medications")
public class CaregiverMedicationController {

    private final PatientMedicationService patientMedicationService;

    @PostMapping
    public ResponseEntity<MedicationResponse> createMedication(@Valid @RequestBody MedicationCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(patientMedicationService.createMedication(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MedicationResponse> updateMedication(
            @PathVariable String id,
            @Valid @RequestBody MedicationUpdateRequest request) {
        return ResponseEntity.ok(patientMedicationService.updateMedication(id, request));
    }

    // Scheduling management
    @PostMapping("/{id}/schedules")
    public ResponseEntity<MedicationResponse> addSchedule(
            @PathVariable String id,
            @Valid @RequestBody ScheduleRequest request) {
        return ResponseEntity.ok(patientMedicationService.addMedicationSchedule(request, id));
    }

    @PutMapping("/{id}/schedules/{scheduleId}")
    public ResponseEntity<MedicationResponse> updateSchedule(
            @PathVariable String id,
            @PathVariable String scheduleId,
            @Valid @RequestBody ScheduleRequest request) {
        return ResponseEntity.ok(patientMedicationService.updateMedicationSchedule(id, scheduleId, request));
    }

    @DeleteMapping("/{id}/schedules/{scheduleId}")
    public ResponseEntity<MedicationResponse> deleteSchedule(
            @PathVariable String id,
            @PathVariable String scheduleId) {
        return ResponseEntity.ok(patientMedicationService.deleteMedicationSchedule(id, scheduleId));
    }

    @PostMapping("/{id}/schedules/{scheduleId}/times")
    public ResponseEntity<MedicationResponse> addTime(
            @PathVariable String id,
            @PathVariable String scheduleId,
            @Valid @RequestBody ScheduleTimeRequest request) {
        return ResponseEntity.ok(patientMedicationService.addScheduleTime(id, scheduleId, request));
    }

    @PutMapping("/{id}/schedules/{scheduleId}/times/{timeId}")
    public ResponseEntity<MedicationResponse> updateTime(
            @PathVariable String id,
            @PathVariable String scheduleId,
            @PathVariable String timeId,
            @Valid @RequestBody ScheduleTimeRequest request) {
        return ResponseEntity.ok(patientMedicationService.updateScheduleTime(id, scheduleId, timeId, request));
    }

    @DeleteMapping("/{id}/schedules/{scheduleId}/times/{timeId}")
    public ResponseEntity<MedicationResponse> deleteTime(
            @PathVariable String id,
            @PathVariable String scheduleId,
            @PathVariable String timeId) {
        return ResponseEntity.ok(patientMedicationService.deleteScheduleTime(id, scheduleId, timeId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        patientMedicationService.deletePatientMedication(id);
        return ResponseEntity.noContent().build();
    }
}
