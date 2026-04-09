package ct01.n07.backend.dto.medication;

import ct01.n07.backend.model.enums.MealRelation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class MedicationUpdateRequest {
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

    @Valid
    private List<MedScheduleRequest> schedules;

    @Min(value = 1, message = "totalQuantity must be greater than 0")
    private Integer totalQuantity;

    private Boolean isActive;
}

