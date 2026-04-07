package ct01.n07.backend.controller;

import ct01.n07.backend.dto.patientMedication.MedicationResponse;
import ct01.n07.backend.service.PatientMedicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({ "/medications" })
@RequiredArgsConstructor
public class PatientMedicationController {

    private final PatientMedicationService patientMedicationService;

    @GetMapping(params = "patientId")
    public ResponseEntity<Page<MedicationResponse>> getActiveMedications(
            @RequestParam String patientId,
            @RequestParam(required = false) Boolean isActive,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(patientMedicationService.getMedications(patientId, isActive, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MedicationResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(patientMedicationService.getPatientMedicationById(id));
    }

}
