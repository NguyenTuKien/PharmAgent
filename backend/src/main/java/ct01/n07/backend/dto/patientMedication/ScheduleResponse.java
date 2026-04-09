package ct01.n07.backend.dto.patientMedication;

import ct01.n07.backend.model.enums.ScheduleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleResponse {
    private String id;
    private ScheduleType scheduleType;
    private Integer frequencyInterval;
    private List<String> daysOfWeek;
    private boolean reminderEnabled;
    private int reminderMinutesBefore;
    private String note;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isActive;
    private List<ScheduleTimeResponse> scheduleTimes;
}
