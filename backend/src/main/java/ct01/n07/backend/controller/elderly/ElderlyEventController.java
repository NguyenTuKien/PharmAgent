package ct01.n07.backend.controller.elderly;

import ct01.n07.backend.dto.event.DoseStatusUpdateRequest;
import ct01.n07.backend.dto.event.EventDoseResponse;
import ct01.n07.backend.facade.DoseConfirmationFacade;
import ct01.n07.backend.service.EventDoseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/elderly/events")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ELDERLY')")
public class ElderlyEventController {

    private final DoseConfirmationFacade doseConfirmationFacade;
    private final EventDoseService eventDoseService;

    /**
     * POST /elderly/doses/{id}/confirm
     * Xác nhận đã uống thuốc. Nếu quá hạn (now > scheduledAt) thì status = OVERDUE, ngược lại là TAKEN.
     */
    @PostMapping("/{id}/confirm")
    public ResponseEntity<EventDoseResponse> confirmDose(@PathVariable String id) {
        return ResponseEntity.ok(doseConfirmationFacade.confirmDose(id));
    }

    /**
     * PUT /elderly/events/{id}/status
     * Elderly tự cập nhật trạng thái cữ thuốc của chính mình.
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<EventDoseResponse> updateDoseStatus(
            @PathVariable String id,
            @Valid @RequestBody DoseStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(eventDoseService.updateDoseStatus(id, request));
    }
}
