package ct01.n07.backend.controller;

import ct01.n07.backend.dto.doseEvent.AdherenceResponse;
import ct01.n07.backend.dto.doseEvent.InventoryWarningResponse;
import ct01.n07.backend.facade.StatsFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsFacade statsFacade;

    /**
     * GET /api/stats/adherence
     * Tính tỷ lệ tuân thủ uống thuốc trong khoảng thời gian cho một bệnh nhân.
     * Roles: Authenticated
     */
    @GetMapping("/adherence")
    public ResponseEntity<AdherenceResponse> getAdherence(
            @RequestParam String patientId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(statsFacade.getAdherence(patientId, startDate, endDate));
    }

    /**
     * GET /api/stats/active-patients-count
     * Thống kê số lượng người đang sử dụng thuốc (có ít nhất 1 thuốc đang hoạt động).
     * Roles: Authenticated
     */
    @GetMapping("/active-patients-count")
    public ResponseEntity<Long> getActivePatientsCount() {
        return ResponseEntity.ok(statsFacade.getActivePatientsCount());
    }

    @GetMapping("/inventory-warnings")
    public ResponseEntity<List<InventoryWarningResponse>> getInventoryWarnings(@RequestParam String patientId) {
        return ResponseEntity.ok(statsFacade.getInventoryWarnings(patientId));
    }
}
