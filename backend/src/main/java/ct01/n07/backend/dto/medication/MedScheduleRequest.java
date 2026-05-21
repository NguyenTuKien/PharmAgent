package ct01.n07.backend.dto.medication;

import com.fasterxml.jackson.annotation.JsonAlias;
import ct01.n07.backend.model.enums.ScheduleType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class MedScheduleRequest {
    @NotNull(message = "Schedule type is required")
    @JsonAlias("frequencyType")
    private ScheduleType scheduleType;

    @Min(value = 1, message = "frequencyInterval must be at least 1")
    @Max(value = 365, message = "frequencyInterval cannot exceed 365")
    @JsonAlias("interval")
    private Integer frequencyInterval;

    @Size(max = 7, message = "daysOfWeek cannot exceed 7 days")
    private List<String> daysOfWeek;

    private Boolean reminderEnabled;

    @Min(value = 0, message = "reminderMinutesBefore cannot be negative")
    private Integer reminderMinutesBefore;

    private String note;

    @NotNull(message = "startDate is required")
    private LocalDate startDate;

    private LocalDate endDate;

    private Boolean isActive;

    @Valid
    @Size(min = 1, message = "Mỗi lịch cần ít nhất một khung giờ uống")
    @JsonAlias("times")
    private List<MedDoseRequest> medDoseRequests;
}



