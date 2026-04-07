package ct01.n07.backend.dto.doseEvent;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class MedicationCreateRequest {
    @NotBlank(message = "patientId is required")
    private String patientId;

    @NotBlank(message = "pillId is required")
    private String pillId;

    @NotEmpty(message = "schedules is required")
    private List<@NotBlank(message = "schedule time cannot be blank") String> schedules;

    @NotNull(message = "totalQuantity is required")
    @Min(value = 1, message = "totalQuantity must be greater than 0")
    private Integer totalQuantity;
}

