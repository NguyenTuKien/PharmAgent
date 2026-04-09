package ct01.n07.backend.controller.caregiver;

import ct01.n07.backend.dto.doseEvent.DoseEventResponse;
import ct01.n07.backend.dto.doseEvent.DoseStatusUpdateRequest;
import ct01.n07.backend.service.DoseEventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/caregiver/doses")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CAREGIVER')")
public class CaregiverDoseController {

    private final DoseEventService doseEventService;

    /**
     * PUT /caregiver/doses/{id}/status
     * Cập nhật trạng thái thủ công (TAKEN, SKIPPED, v.v.) dành cho người chăm sóc.
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<DoseEventResponse> updateDoseStatus(
            @PathVariable String id,
            @Valid @RequestBody DoseStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(doseEventService.updateDoseStatus(id, request));
    }
}
