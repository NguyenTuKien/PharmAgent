package ct01.n07.backend.dto.stats;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MedicationDoseStatsResponse {
    private String nickname;
    private long takenCount;
}
