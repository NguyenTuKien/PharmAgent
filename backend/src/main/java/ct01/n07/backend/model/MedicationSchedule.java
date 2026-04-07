package ct01.n07.backend.model;

import ct01.n07.backend.model.enums.ScheduleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicationSchedule {
    @Builder.Default
    private String id = new ObjectId().toString();
    private ScheduleType scheduleType;
    private Integer frequencyInterval;
    private List<String> daysOfWeek; // VD: ["MON", "WED"] thay vì chuỗi String cách nhau dấu phẩy
    private boolean reminderEnabled;
    private int reminderMinutesBefore;
    private String note;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean isActive;

    // Embedded Array: Các khung giờ trong 1 lịch
    private List<ScheduleTime> scheduleTimeList;
}
