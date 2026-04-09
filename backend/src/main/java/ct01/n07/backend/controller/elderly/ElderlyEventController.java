package ct01.n07.backend.controller.elderly;

import ct01.n07.backend.dto.event.EventDoseResponse;
import ct01.n07.backend.facade.DoseConfirmationFacade;
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

    /**
     * POST /elderly/doses/{id}/confirm
     * Xác nhận đã uống thuốc. Nếu quá hạn (now > scheduledAt) thì status = OVERDUE, ngược lại là TAKEN.
     */
    @PostMapping("/{id}/confirm")
    public ResponseEntity<EventDoseResponse> confirmDose(@PathVariable String id) {
        return ResponseEntity.ok(doseConfirmationFacade.confirmDose(id));
    }
}
