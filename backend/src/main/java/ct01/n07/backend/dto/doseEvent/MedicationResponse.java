package ct01.n07.backend.dto.doseEvent;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MedicationResponse {
    private String id;
    private String patientId;
    private String pillId;
    private List<String> schedules;
    private Integer totalQuantity;
    private boolean isActive;
}

