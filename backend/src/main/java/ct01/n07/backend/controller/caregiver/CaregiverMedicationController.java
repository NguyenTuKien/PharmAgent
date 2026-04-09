package ct01.n07.backend.controller.caregiver;

import ct01.n07.backend.dto.medication.*;
import ct01.n07.backend.service.MedicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/caregiver/medications")
public class CaregiverMedicationController {

    private final MedicationService medicationService;

    @PostMapping
    public ResponseEntity<MedicationResponse> createMedication(@Valid @RequestBody MedicationCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(medicationService.createMedication(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MedicationResponse> updateMedication(
            @PathVariable String id,
            @Valid @RequestBody MedicationUpdateRequest request) {
        return ResponseEntity.ok(medicationService.updateMedication(id, request));
    }

    // Scheduling management
    @PostMapping("/{id}/schedules")
    public ResponseEntity<MedicationResponse> addSchedule(
            @PathVariable String id,
            @Valid @RequestBody MedScheduleRequest request) {
        return ResponseEntity.ok(medicationService.addMedicationSchedule(request, id));
    }

    @PutMapping("/{id}/schedules/{scheduleId}")
    public ResponseEntity<MedicationResponse> updateSchedule(
            @PathVariable String id,
            @PathVariable String scheduleId,
            @Valid @RequestBody MedScheduleRequest request) {
        return ResponseEntity.ok(medicationService.updateMedicationSchedule(id, scheduleId, request));
    }

    @DeleteMapping("/{id}/schedules/{scheduleId}")
    public ResponseEntity<MedicationResponse> deleteSchedule(
            @PathVariable String id,
            @PathVariable String scheduleId) {
        return ResponseEntity.ok(medicationService.deleteMedicationSchedule(id, scheduleId));
    }

    @PostMapping("/{id}/schedules/{scheduleId}/times")
    public ResponseEntity<MedicationResponse> addTime(
            @PathVariable String id,
            @PathVariable String scheduleId,
            @Valid @RequestBody MedDoseRequest request) {
        return ResponseEntity.ok(medicationService.addScheduleTime(id, scheduleId, request));
    }

    @PutMapping("/{id}/schedules/{scheduleId}/times/{timeId}")
    public ResponseEntity<MedicationResponse> updateTime(
            @PathVariable String id,
            @PathVariable String scheduleId,
            @PathVariable String timeId,
            @Valid @RequestBody MedDoseRequest request) {
        return ResponseEntity.ok(medicationService.updateScheduleTime(id, scheduleId, timeId, request));
    }

    @DeleteMapping("/{id}/schedules/{scheduleId}/times/{timeId}")
    public ResponseEntity<MedicationResponse> deleteTime(
            @PathVariable String id,
            @PathVariable String scheduleId,
            @PathVariable String timeId) {
        return ResponseEntity.ok(medicationService.deleteScheduleTime(id, scheduleId, timeId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        medicationService.deleteMedication(id);
        return ResponseEntity.noContent().build();
    }
}
