package ct01.web.backend.dto.doseEvent;

import ct01.web.backend.model.enums.MealRelation;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class PatientMedicationRequest {

    @NotBlank(message = "ID Bệnh nhân không được để trống")
    private String patientId;

    @NotBlank(message = "ID Thuốc không được để trống")
    private String pillId;

    @Size(max = 255, message = "Tên gợi nhớ không được vượt quá 255 ký tự")
    private String nickname;

    @NotNull(message = "Liều lượng không được để trống")
    @DecimalMin(value = "0.01", message = "Liều lượng phải lớn hơn 0")
    private BigDecimal dosageAmount;

    @NotBlank(message = "Đơn vị tính không được để trống (VD: Viên, ml, gói)")
    @Size(max = 50, message = "Đơn vị tính không được vượt quá 50 ký tự")
    private String dosageUnit;

    @NotBlank(message = "Đường dùng không được để trống (VD: Uống, bôi, tiêm)")
    @Size(max = 50, message = "Đường dùng không được vượt quá 50 ký tự")
    private String route;

    // Enum thường không cần @NotBlank, nếu bắt buộc phải có thì dùng @NotNull
    private MealRelation mealRelation;

    @Size(max = 500, message = "Hướng dẫn sử dụng quá dài (tối đa 500 ký tự)")
    private String instruction;

    @Size(max = 255, message = "Tên người kê đơn không được vượt quá 255 ký tự")
    private String prescribedBy;

    @Size(max = 255, message = "Mục đích sử dụng không được vượt quá 255 ký tự")
    private String purpose;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDate startDate;

    // Ngày kết thúc có thể để trống (đối với thuốc mãn tính uống vô thời hạn)
    private LocalDate endDate;

    // boolean là kiểu nguyên thủy (primitive) nên mặc định là false nếu không truyền, không cần validate Null.
    private boolean isPrn;

    // maxPerDay cực kỳ quan trọng đối với thuốc PRN (uống khi cần) để tránh quá liều
    @Min(value = 1, message = "Số lần uống tối đa trong ngày phải lớn hơn 0")
    @Max(value = 50, message = "Số lần uống tối đa trong ngày không hợp lý")
    private Integer maxPerDay;
}