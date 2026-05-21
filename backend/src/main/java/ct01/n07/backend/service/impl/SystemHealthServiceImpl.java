package ct01.n07.backend.service.impl;

import ct01.n07.backend.dto.health.ServiceHealthItem;
import ct01.n07.backend.dto.health.SystemHealthResponse;
import ct01.n07.backend.service.SystemHealthService;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class SystemHealthServiceImpl implements SystemHealthService {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${server.port:8080}")
    private int serverPort;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Value("${management.endpoints.web.base-path:/actuator}")
    private String actuatorBasePath;

    @Value("${cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${cloudinary.api-key:}")
    private String apiKey;

    @Value("${cloudinary.api-secret:}")
    private String apiSecret;

    @Override
    public SystemHealthResponse getSystemHealth() {
        Map<String, Object> health = fetchActuatorHealth();
        Map<String, Object> components = extractComponents(health);

        List<ServiceHealthItem> services = List.of(
                buildItem("MongoDB", findComponent(components, "mongo", "mongoDb", "mongodb"),
                        "MongoDB connection is healthy",
                        "MongoDB connection failed"),
                buildItem("Redis", findComponent(components, "redis"),
                        "Redis connection is healthy",
                        "Redis connection failed"),
                buildItem("RabbitMQ", findComponent(components, "rabbit", "rabbitmq"),
                        "RabbitMQ connection is healthy",
                        "RabbitMQ connection failed"),
                buildCloudinaryItem()
        );

        return SystemHealthResponse.builder()
                .updatedAt(LocalDateTime.now(DEFAULT_ZONE))
                .services(services)
                .build();
    }

    private Map<String, Object> fetchActuatorHealth() {
        try {
            return restTemplate.getForObject(buildActuatorHealthUrl(), Map.class);
        } catch (Exception ex) {
            return null;
        }
    }

    private Map<String, Object> extractComponents(Map<String, Object> health) {
        if (health == null) {
            return Map.of();
        }
        Object rawComponents = health.get("components");
        if (rawComponents instanceof Map<?, ?> componentMap) {
            return (Map<String, Object>) componentMap;
        }
        return Map.of();
    }

    private Map<String, Object> findComponent(Map<String, Object> components, String... keys) {
        for (String key : keys) {
            Object component = components.get(key);
            if (component != null) {
                return castComponent(component);
            }
        }
        return null;
    }

    private ServiceHealthItem buildItem(
            String name,
            Map<String, Object> component,
            String upMessage,
            String downMessage
    ) {
        String status = component != null ? asString(component.get("status")) : "UNKNOWN";
        String message = resolveMessage(status, component, upMessage, downMessage);
        return ServiceHealthItem.builder()
                .name(name)
                .status(status)
                .message(message)
                .build();
    }

    private String resolveMessage(String status, Map<String, Object> component, String upMessage, String downMessage) {
        if ("UP".equalsIgnoreCase(status)) {
            return upMessage;
        }

        String detailMessage = extractDetailMessage(component);
        if (detailMessage != null) {
            return detailMessage;
        }

        if ("DOWN".equalsIgnoreCase(status)) {
            return downMessage;
        }

        return "Unknown";
    }

    private String extractDetailMessage(Map<String, Object> component) {
        if (component == null) {
            return null;
        }
        Object details = component.get("details");
        if (!(details instanceof Map<?, ?> detailMap)) {
            return null;
        }
        Object message = detailMap.get("message");
        if (message instanceof String text && !text.isBlank()) {
            return text;
        }
        Object error = detailMap.get("error");
        if (error instanceof String text && !text.isBlank()) {
            return text;
        }
        return null;
    }

    private Map<String, Object> castComponent(Object component) {
        if (component instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return null;
    }

    private String asString(Object value) {
        if (value == null) {
            return "UNKNOWN";
        }
        return String.valueOf(value);
    }

    private ServiceHealthItem buildCloudinaryItem() {
        List<String> missing = new ArrayList<>();
        if (cloudName == null || cloudName.isBlank()) {
            missing.add("cloud-name");
        }
        if (apiKey == null || apiKey.isBlank()) {
            missing.add("api-key");
        }
        if (apiSecret == null || apiSecret.isBlank()) {
            missing.add("api-secret");
        }

        if (missing.isEmpty()) {
            return ServiceHealthItem.builder()
                    .name("Cloudinary")
                    .status("UP")
                    .message("Cloudinary config is available")
                    .build();
        }

        return ServiceHealthItem.builder()
                .name("Cloudinary")
                .status("UNKNOWN")
                .message("Missing Cloudinary config: " + String.join(", ", missing))
                .build();
    }

    private String buildActuatorHealthUrl() {
        String normalizedContext = normalizePath(contextPath);
        String normalizedBasePath = normalizePath(actuatorBasePath);
        return "http://localhost:" + serverPort + normalizedContext + normalizedBasePath + "/health";
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        String trimmed = path.trim();
        if (!trimmed.startsWith("/")) {
            trimmed = "/" + trimmed;
        }
        if (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
