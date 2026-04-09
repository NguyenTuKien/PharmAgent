package ct01.n07.backend.dto.event;

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
public class EventDoseResponse {
    private String id;
    private String medicationId;
    private String scheduleId;
    private String medDoseId;
    private LocalDateTime scheduledAt;
    private DoseStatus status;
    private LocalDateTime takenAt;
    private String confirmedBy;
    private String note;
}

