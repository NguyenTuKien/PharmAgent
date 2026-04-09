package ct01.n07.backend.dto.patientMedication;

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
public class ScheduleTimeResponse {
    private String id;
    private LocalTime takenTime;
    private BigDecimal quantity;
}
