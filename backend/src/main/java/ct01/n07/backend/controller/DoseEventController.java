package ct01.n07.backend.controller;

import ct01.n07.backend.dto.doseEvent.DoseEventResponse;
import ct01.n07.backend.service.DoseEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/doses")
@RequiredArgsConstructor
public class DoseEventController {

    private final DoseEventService doseEventService;

    @GetMapping("/today")
    @PreAuthorize("hasAnyRole('CAREGIVER', 'ELDERLY')")
    public ResponseEntity<Page<DoseEventResponse>> getTodayDoses(
            @RequestParam String patientId,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(doseEventService.getTodayDoses(patientId, pageable));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('CAREGIVER', 'ELDERLY')")
    public ResponseEntity<Page<DoseEventResponse>> getPendingDoses(
            @RequestParam String patientId,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(doseEventService.getPendingDoses(patientId, pageable));
    }

    @GetMapping("/processed")
    @PreAuthorize("hasAnyRole('CAREGIVER', 'ELDERLY')")
    public ResponseEntity<Page<DoseEventResponse>> getProcessedDoses(
            @RequestParam String patientId,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(doseEventService.getProcessedDoses(patientId, pageable));
    }

}
