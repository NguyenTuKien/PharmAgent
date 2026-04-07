package ct01.n07.backend.controller;

import ct01.n07.backend.dto.doseEvent.DoseEventResponse;
import ct01.n07.backend.dto.doseEvent.DoseStatusUpdateRequest;
import ct01.n07.backend.service.DoseEventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/doses")
@RequiredArgsConstructor
public class DoseEventController {

    private final DoseEventService doseEventService;

    /**
     * GET /api/doses/today
     * Lấy timeline cữ thuốc trong ngày cho một bệnh nhân.
     * Roles: CAREGIVER, ELDERLY
     *
     * @param patientId ID của bệnh nhân (UserProfile ID)
     * @param date      Ngày cần lấy timeline (ISO 8601, mặc định là hôm nay)
     */
    @GetMapping("/today")
    @PreAuthorize("hasAnyRole('CAREGIVER', 'ELDERLY')")
    public ResponseEntity<List<DoseEventResponse>> getTodayTimeline(
            @RequestParam String patientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        return ResponseEntity.ok(doseEventService.getTodayTimeline(patientId, targetDate));
    }

    /**
     * PUT /api/doses/{id}/status
     * Cập nhật trạng thái một cữ thuốc (TAKEN / SKIPPED / REMIND).
     * Roles: CAREGIVER, ELDERLY
     *
     * @param id      ID của DoseEvent
     * @param request Body chứa status mới và note tùy chọn
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('CAREGIVER', 'ELDERLY')")
    public ResponseEntity<DoseEventResponse> updateDoseStatus(
            @PathVariable String id,
            @Valid @RequestBody DoseStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(doseEventService.updateDoseStatus(id, request));
    }
}
