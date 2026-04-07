package ct01.n07.backend.dto.doseEvent;

import ct01.n07.backend.model.enums.DoseStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoseStatusUpdateRequest {

    @NotNull(message = "Trạng thái cữ thuốc (status) không được để trống")
    private DoseStatus status;

    @Size(max = 500, message = "Ghi chú không được vượt quá 500 ký tự")
    private String note;
}
