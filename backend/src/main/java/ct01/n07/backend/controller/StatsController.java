package ct01.n07.backend.controller;

import ct01.n07.backend.dto.stats.MedicationDoseStatsResponse;
import ct01.n07.backend.dto.stats.AdherenceResponse;
import ct01.n07.backend.dto.stats.InventoryWarningResponse;
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
     * GET /api/stats/doses-by-medication
     * Thống kê số lượng thuốc đã uống (TAKEN) của từng loại thuốc trong một khoảng
     * thời gian.
     */
    @GetMapping("/doses-by-medication")
    public ResponseEntity<List<MedicationDoseStatsResponse>> getTakenDosesByMedication(
            @RequestParam String patientId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(statsFacade.getTakenDosesByMedication(patientId, startDate, endDate));
    }

    @GetMapping("/inventory-warnings")
    public ResponseEntity<List<InventoryWarningResponse>> getInventoryWarnings(@RequestParam String patientId) {
        return ResponseEntity.ok(statsFacade.getInventoryWarnings(patientId));
    }
}
