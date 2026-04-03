package ct01.web.backend.controller;

import ct01.web.backend.dto.doseEvent.MedicationScheduleRequest;
import ct01.web.backend.dto.doseEvent.PatientMedicationRequest;
import ct01.web.backend.dto.doseEvent.ScheduleTimeRequest;
import ct01.web.backend.model.PatientMedication;
import ct01.web.backend.service.PatientMedicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/patient-medications")
@RequiredArgsConstructor
public class PatientMedicationController {

    private final PatientMedicationService patientMedicationService;

    @GetMapping
    public ResponseEntity<List<PatientMedication>> getAll() {
        return ResponseEntity.ok(patientMedicationService.getAllPatientMedications());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PatientMedication> getById(@PathVariable String id) {
        return ResponseEntity.ok(patientMedicationService.getPatientMedicationById(id));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<PatientMedication>> getByPatientId(@PathVariable String patientId) {
        return ResponseEntity.ok(patientMedicationService.getPatientMedicationsByPatientId(patientId));
    }

    @PostMapping
    public ResponseEntity<PatientMedication> create(@Valid @RequestBody PatientMedicationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(patientMedicationService.createPatientMedication(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PatientMedication> update(@PathVariable String id, @Valid @RequestBody PatientMedicationRequest request) {
        return ResponseEntity.ok(patientMedicationService.updatePatientMedication(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        patientMedicationService.deletePatientMedication(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/schedules")
    public ResponseEntity<PatientMedication> addSchedule(
            @PathVariable String id,
            @Valid @RequestBody MedicationScheduleRequest request) {
        request.setPatientMedicationId(id);
        return ResponseEntity.ok(patientMedicationService.addMedicationSchedule(request));
    }

    @PutMapping("/{id}/schedules/{scheduleId}")
    public ResponseEntity<PatientMedication> updateSchedule(
            @PathVariable String id,
            @PathVariable String scheduleId,
            @Valid @RequestBody MedicationScheduleRequest request) {
        request.setPatientMedicationId(id);
        return ResponseEntity.ok(patientMedicationService.updateMedicationSchedule(id, scheduleId, request));
    }

    @DeleteMapping("/{id}/schedules/{scheduleId}")
    public ResponseEntity<PatientMedication> deleteSchedule(
            @PathVariable String id,
            @PathVariable String scheduleId) {
        return ResponseEntity.ok(patientMedicationService.deleteMedicationSchedule(id, scheduleId));
    }

    @PostMapping("/{id}/schedules/{scheduleId}/times")
    public ResponseEntity<PatientMedication> addTime(
            @PathVariable String id,
            @PathVariable String scheduleId,
            @Valid @RequestBody ScheduleTimeRequest request) {
        return ResponseEntity.ok(patientMedicationService.addScheduleTime(id, scheduleId, request));
    }

    @PutMapping("/{id}/schedules/{scheduleId}/times/{timeId}")
    public ResponseEntity<PatientMedication> updateTime(
            @PathVariable String id,
            @PathVariable String scheduleId,
            @PathVariable String timeId,
            @Valid @RequestBody ScheduleTimeRequest request) {
        return ResponseEntity.ok(patientMedicationService.updateScheduleTime(id, scheduleId, timeId, request));
    }

    @DeleteMapping("/{id}/schedules/{scheduleId}/times/{timeId}")
    public ResponseEntity<PatientMedication> deleteTime(
            @PathVariable String id,
            @PathVariable String scheduleId,
            @PathVariable String timeId) {
        return ResponseEntity.ok(patientMedicationService.deleteScheduleTime(id, scheduleId, timeId));
    }
}
