package ct01.n07.backend.dto.patientMedication;

import ct01.n07.backend.model.enums.ScheduleType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class MedicationScheduleRequest {
    @Size(max = 50, message = "ID đơn thuốc (patientMedicationId) không được vượt quá 50 ký tự")
    String patientMedicationId;

    @NotNull(message = "Loại lịch trình (scheduleType) không được để trống")
    private ScheduleType scheduleType;

    @Min(value = 1, message = "Khoảng thời gian lặp (frequencyInterval) phải từ 1 ngày trở lên")
    @Max(value = 365, message = "Khoảng thời gian lặp (frequencyInterval) không được vượt quá 365 ngày")
    private Integer frequencyInterval;

    @Size(max = 7, message = "Danh sách các ngày trong tuần không được phép vượt quá 7 ngày")
    private List<@Size(max = 10, message = "Tên ký hiệu ngày không hợp lệ") String> daysOfWeek;

    private boolean reminderEnabled;

    @Min(value = 0, message = "Thời gian nhắc nhở trước (phút) không được là số âm")
    @Max(value = 1440, message = "Thời gian nhắc nhở trước không được vượt quá 24 giờ (1440 phút)")
    private int reminderMinutesBefore;

    @Size(max = 500, message = "Ghi chú không được vượt quá 500 ký tự")
    private String note;

    @NotNull(message = "Ngày bắt đầu (startDate) không được để trống")
    private LocalDate startDate;

    private LocalDate endDate;

    @Valid
    @Size(max = 24, message = "Một lịch trình không được vượt quá 24 khung giờ uống thuốc mỗi ngày")
    private List<@NotNull(message = "Khung giờ uống thuốc không được chứa giá trị null") ScheduleTimeRequest> scheduleTimeRequests;
}
