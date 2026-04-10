package ct01.n07.backend.dto.medication;

import ct01.n07.backend.model.enums.MealRelation;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class MedicationResponse {
    private String id;
    private String patientId;
    private String pillId;

    private String nickname;
    private BigDecimal dosageAmount;
    private String dosageUnit;
    private String route;
    private MealRelation mealRelation;
    private String instruction;
    private String prescribedBy;
    private String purpose;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer maxPerDay;

    private List<MedScheduleResponse> schedules;
    private Integer totalQuantity;
    private Boolean isActive;

    private Instant createdAt;
    private Instant updatedAt;
}

