package ct01.n07.backend.dto.patientMedication;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleTimeRequest {
    @NotNull(message = "Thời gian uống (takenTime) không được để trống")
    private LocalTime takenTime;

    @NotNull(message = "Số lượng không được để trống")
    @DecimalMin(value = "0.01", message = "Số lượng thuốc mỗi lần uống phải lớn hơn 0")
    @DecimalMax(value = "100.00", message = "Số lượng thuốc mỗi lần uống vượt quá giới hạn (tối đa 100)")
    private BigDecimal quantity;
}
