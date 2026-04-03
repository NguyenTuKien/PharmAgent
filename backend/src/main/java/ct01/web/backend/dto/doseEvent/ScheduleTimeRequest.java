package ct01.web.backend.dto.doseEvent;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
@Builder
public class ScheduleTimeRequest {
    @NotNull(message = "Thời gian uống không được để trống")
    private LocalTime takenTime;

    @NotNull(message = "Số lượng không được để trống")
    private BigDecimal quantity;
}
