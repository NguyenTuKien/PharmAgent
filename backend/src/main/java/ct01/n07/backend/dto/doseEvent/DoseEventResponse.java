package ct01.n07.backend.dto.doseEvent;

import ct01.n07.backend.model.enums.DoseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoseEventResponse {
    private String id;
    private String patientMedicationId;
    private String scheduleId;
    private String scheduleTimeId;
    private LocalDateTime scheduledAt;
    private DoseStatus status;
    private LocalDateTime takenAt;
    private String confirmedBy;
    private String note;
}
