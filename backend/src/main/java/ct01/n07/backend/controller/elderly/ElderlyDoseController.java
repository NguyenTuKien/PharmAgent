package ct01.n07.backend.controller.elderly;

import ct01.n07.backend.dto.doseEvent.DoseEventResponse;
import ct01.n07.backend.service.DoseEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/elderly/doses")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ELDERLY')")
public class ElderlyDoseController {

    private final DoseEventService doseEventService;

    /**
     * POST /elderly/doses/{id}/confirm
     * Xác nhận đã uống thuốc. Nếu quá hạn (now > scheduledAt) thì status = OVERDUE, ngược lại là TAKEN.
     */
    @PostMapping("/{id}/confirm")
    public ResponseEntity<DoseEventResponse> confirmDose(@PathVariable String id) {
        return ResponseEntity.ok(doseEventService.confirmDose(id));
    }
}
