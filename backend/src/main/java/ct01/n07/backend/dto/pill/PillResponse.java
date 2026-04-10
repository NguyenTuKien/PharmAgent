package ct01.n07.backend.dto.pill;

import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PillResponse {
    private String id;
    private String name;
    private String genericName;
    private String brandName;
    private String strength;
    private String dosageForm;
    private String color;
    private String shape;
    private String description;
    private String usageInstructions;
    private String warning;
    private String sideEffects;
    private String manufacturer;
    private boolean isActive;
    private List<PillImageResponse> images;
    private Instant createdAt;
    private Instant updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PillImageResponse {
        private String id;
        private String imageUrl;
        private String viewType;
        private boolean isPrimary;
    }
}
