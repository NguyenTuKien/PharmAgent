package ct01.n07.backend.controller.caregiver;

import ct01.n07.backend.dto.event.DoseStatusUpdateRequest;
import ct01.n07.backend.dto.event.EventDoseResponse;
import ct01.n07.backend.service.EventDoseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/caregiver/events")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CAREGIVER')")
public class CaregiverEventController {

    private final EventDoseService eventDoseService;

    /**
     * PUT /caregiver/doses/{id}/status
     * Cập nhật trạng thái thủ công (TAKEN, SKIPPED, v.v.) dành cho người chăm sóc.
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<EventDoseResponse> updateDoseStatus(
            @PathVariable String id,
            @Valid @RequestBody DoseStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(eventDoseService.updateDoseStatus(id, request));
    }
}
