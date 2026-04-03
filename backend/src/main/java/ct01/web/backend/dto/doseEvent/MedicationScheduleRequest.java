package ct01.web.backend.dto.doseEvent;

import ct01.web.backend.model.enums.ScheduleType;
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
    String patientMedicationId;

    @NotNull(message = "Loại lịch trình (scheduleType) không được để trống")
    private ScheduleType scheduleType;

    // Có thể null (nếu scheduleType không yêu cầu), nhưng nếu truyền vào thì phải >= 1
    @Min(value = 1, message = "Khoảng thời gian lặp (frequencyInterval) phải lớn hơn hoặc bằng 1")
    private Integer frequencyInterval;

    // Giới hạn tối đa 7 ngày trong tuần.
    // Lưu ý: Kiểm tra tính hợp lệ của chuỗi (VD: "MON", "TUE") nên xử lý ở tầng Service hoặc dùng Custom Validator.
    @Size(max = 7, message = "Danh sách các ngày trong tuần không được vượt quá 7")
    private List<String> daysOfWeek;

    // Kiểu boolean nguyên thủy (primitive) mặc định là false, không cần validate Null
    private boolean reminderEnabled;

    // Tránh việc user nhập số âm hoặc số quá vô lý (VD: nhắc trước 24h = 1440 phút)
    @Min(value = 0, message = "Thời gian nhắc nhở trước (phút) không được là số âm")
    @Max(value = 1440, message = "Thời gian nhắc nhở không được vượt quá 24 giờ (1440 phút)")
    private int reminderMinutesBefore;

    @Size(max = 500, message = "Ghi chú không được vượt quá 500 ký tự")
    private String note;

    @NotNull(message = "Ngày bắt đầu (startDate) không được để trống")
    private LocalDate startDate;

    // Ngày kết thúc có thể để trống (lặp vô thời hạn)
    private LocalDate endDate;

    // Danh sách khung giờ uống thuốc
    private List<ScheduleTimeRequest> scheduleTimeRequests;
}
