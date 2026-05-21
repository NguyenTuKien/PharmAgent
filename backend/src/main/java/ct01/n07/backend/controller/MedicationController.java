package ct01.n07.backend.controller;

import ct01.n07.backend.dto.medication.MedicationCreateRequest;
import ct01.n07.backend.dto.medication.MedicationResponse;
import ct01.n07.backend.dto.medication.MedicationUpdateRequest;
import ct01.n07.backend.facade.MedicationFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/medications")
@RequiredArgsConstructor
public class MedicationController {

    private final MedicationFacade medicationFacade;

    @PostMapping
    public ResponseEntity<MedicationResponse> createMedication(@Valid @RequestBody MedicationCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(medicationFacade.createMedication(request));
    }

    @GetMapping
    public ResponseEntity<Page<MedicationResponse>> getActiveMedications(
            @RequestParam String patientId,
            @RequestParam(required = false) Boolean isActive,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(medicationFacade.getMedications(patientId, isActive, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MedicationResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(medicationFacade.getMedicationById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MedicationResponse> updateMedication(
            @PathVariable String id,
            @Valid @RequestBody MedicationUpdateRequest request) {
        return ResponseEntity.ok(medicationFacade.updateMedication(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        medicationFacade.deleteMedication(id);
        return ResponseEntity.noContent().build();
    }

}

