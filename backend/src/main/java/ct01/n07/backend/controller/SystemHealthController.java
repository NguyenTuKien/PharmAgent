package ct01.n07.backend.controller;

import ct01.n07.backend.dto.health.SystemHealthResponse;
import ct01.n07.backend.service.SystemHealthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/system")
public class SystemHealthController {

    private final SystemHealthService systemHealthService;

    @GetMapping("/health")
    public ResponseEntity<SystemHealthResponse> getSystemHealth() {
        return ResponseEntity.ok(systemHealthService.getSystemHealth());
    }
}
