package ct01.n07.backend.dto.doseEvent;

import jakarta.validation.constraints.Min;
import lombok.Data;

import java.util.List;

@Data
public class MedicationUpdateRequest {
    private String pillId;

    private List<String> schedules;

    @Min(value = 1, message = "totalQuantity must be greater than 0")
    private Integer totalQuantity;

    private Boolean isActive;
}

