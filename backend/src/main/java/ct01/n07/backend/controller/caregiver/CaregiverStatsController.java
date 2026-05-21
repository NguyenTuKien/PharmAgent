package ct01.n07.backend.controller.caregiver;

import ct01.n07.backend.dto.stats.CaregiverOverviewResponse;
import ct01.n07.backend.facade.CaregiverStatsFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/caregiver/stats")
public class CaregiverStatsController {

    private final CaregiverStatsFacade caregiverStatsFacade;

    @GetMapping("/overview")
    public ResponseEntity<CaregiverOverviewResponse> getCaregiverOverview() {
        return ResponseEntity.ok(caregiverStatsFacade.getCaregiverOverview());
    }
}
