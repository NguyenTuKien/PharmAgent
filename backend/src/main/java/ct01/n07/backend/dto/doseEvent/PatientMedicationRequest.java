package ct01.n07.backend.dto.doseEvent;

import ct01.n07.backend.model.enums.MealRelation;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class PatientMedicationRequest {

    @NotBlank(message = "ID Bệnh nhân không được để trống")
    @Size(max = 50, message = "ID Bệnh nhân không được vượt quá 50 ký tự")
    private String patientId;

    @NotBlank(message = "ID Thuốc không được để trống")
    @Size(max = 50, message = "ID Thuốc không được vượt quá 50 ký tự")
    private String pillId;

    @Size(max = 255, message = "Tên gợi nhớ không được vượt quá 255 ký tự")
    private String nickname;

    @NotNull(message = "Liều lượng không được để trống")
    @DecimalMin(value = "0.01", message = "Liều lượng phải lớn hơn hoặc bằng 0.01")
    @DecimalMax(value = "10000.00", message = "Liều lượng vuợt quá giới hạn an toàn")
    private BigDecimal dosageAmount;

    @NotBlank(message = "Đơn vị tính không được để trống (VD: Viên, ml, gói)")
    @Size(max = 50, message = "Đơn vị tính không được vượt quá 50 ký tự")
    private String dosageUnit;

    @NotBlank(message = "Đường dùng không được để trống (VD: Uống, bôi, tiêm)")
    @Size(max = 50, message = "Đường dùng không được vượt quá 50 ký tự")
    private String route;

    @NotNull(message = "Mối quan hệ với bữa ăn không được để trống")
    private MealRelation mealRelation;

    @Size(max = 500, message = "Hướng dẫn sử dụng quá dài (tối đa 500 ký tự)")
    private String instruction;

    @Size(max = 255, message = "Tên người kê đơn không được vượt quá 255 ký tự")
    private String prescribedBy;

    @Size(max = 255, message = "Mục đích sử dụng không được vượt quá 255 ký tự")
    private String purpose;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDate startDate;

    private LocalDate endDate;

    private boolean isPrn;

    @Min(value = 1, message = "Số lần uống tối đa trong ngày phải từ 1 trở lên")
    @Max(value = 50, message = "Số lần uống tối đa trong ngày quá lớn (tối đa là 50)")
    private Integer maxPerDay;
}