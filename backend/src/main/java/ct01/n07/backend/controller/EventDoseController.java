package ct01.n07.backend.controller;

import ct01.n07.backend.dto.event.EventDoseResponse;
import ct01.n07.backend.service.EventDoseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventDoseController {

    private final EventDoseService eventDoseService;

    @GetMapping("/today")
    @PreAuthorize("hasAnyRole('CAREGIVER', 'ELDERLY')")
    public ResponseEntity<Page<EventDoseResponse>> getTodayDoses(
            @RequestParam String patientId,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(eventDoseService.getTodayDoses(patientId, pageable));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('CAREGIVER', 'ELDERLY')")
    public ResponseEntity<Page<EventDoseResponse>> getPendingDoses(
            @RequestParam String patientId,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(eventDoseService.getPendingDoses(patientId, pageable));
    }

    @GetMapping("/processed")
    @PreAuthorize("hasAnyRole('CAREGIVER', 'ELDERLY')")
    public ResponseEntity<Page<EventDoseResponse>> getProcessedDoses(
            @RequestParam String patientId,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(eventDoseService.getProcessedDoses(patientId, pageable));
    }

}
