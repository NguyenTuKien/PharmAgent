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
    private int taken; // Đúng hạn
    private int overdue; // Quá hạn
    private int missed; // Bỏ lỡ
    private int skipped; // Chủ động bỏ qua
    private int pending; // Sắp tới
    private double adherencePercent;
}
