package ct01.n07.backend.model;

import ct01.n07.backend.model.enums.MealRelation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "patient_medications")
public class Medication {
    @Id
    private String id;

    @Field(targetType = FieldType.OBJECT_ID)
    private String patientId;

<<<<<<< Updated upstream
    @Field(targetType = FieldType.STRING)
=======
>>>>>>> Stashed changes
    private String pillId;

    private String nickname;
    private BigDecimal dosageAmount;
    private String dosageUnit;
    private String route;
    private MealRelation mealRelation;
    private String instruction;
    private String prescribedBy;
    private String purpose;
    private Integer totalQuantity;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean isActive;

    // Embedded Array: Danh sách các quy tắc lặp lịch uống
    private List<MedSchedule> medicationSchedules;

    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;
}
