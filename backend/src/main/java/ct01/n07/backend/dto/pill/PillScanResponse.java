package ct01.n07.backend.dto.pill;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PillScanResponse {
    private String pillId;
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
    private Double confidenceScore;
    private List<String> imageUrls;
}
