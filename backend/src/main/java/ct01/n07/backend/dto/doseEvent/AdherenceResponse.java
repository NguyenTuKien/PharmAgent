package ct01.n07.backend.dto.doseEvent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdherenceResponse {
    private int total;
    private int taken;
    private int skipped;
    private double adherencePercent;
}
