package ct01.n07.backend.dto.patientMedication;

import ct01.n07.backend.model.enums.MealRelation;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class MedicationCreateRequest {
    @NotBlank(message = "patientId is required")
    private String patientId;

    @NotBlank(message = "pillId is required")
    private String pillId;

    @Size(max = 255, message = "Tên gợi nhớ không được vượt quá 255 ký tự")
    private String nickname;

    @NotNull(message = "Liều lượng không được để trống")
    @DecimalMin(value = "0.01", message = "Liều lượng phải lớn hơn hoặc bằng 0.01")
    private BigDecimal dosageAmount;

    @NotBlank(message = "Đơn vị tính không được để trống (VD: Viên, ml, gói)")
    private String dosageUnit;

    @NotBlank(message = "Đường dùng không được để trống (VD: Uống, bôi, tiêm)")
    private String route;

    @NotNull(message = "Mối quan hệ với bữa ăn không được để trống")
    private MealRelation mealRelation;

    @Size(max = 500, message = "Hướng dẫn sử dụng quá dài (tối đa 500 ký tự)")
    private String instruction;

    private String prescribedBy;
    private String purpose;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDate startDate;

    private LocalDate endDate;

    private boolean isPrn;

    @Min(value = 1)
    private Integer maxPerDay;

    @NotEmpty(message = "schedules is required")
    private List<@NotBlank(message = "schedule time cannot be blank") String> schedules;

    @NotNull(message = "totalQuantity is required")
    @Min(value = 1, message = "totalQuantity must be greater than 0")
    private Integer totalQuantity;
}
