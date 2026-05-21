package ct01.n07.backend.service;

import ct01.n07.backend.dto.health.SystemHealthResponse;

public interface SystemHealthService {
    SystemHealthResponse getSystemHealth();
}
