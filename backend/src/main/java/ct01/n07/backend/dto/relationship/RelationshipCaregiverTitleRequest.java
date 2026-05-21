package ct01.n07.backend.dto.relationship;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RelationshipCaregiverTitleRequest {
    @NotBlank(message = "Cách gọi người hỗ trợ không được để trống")
    @Size(max = 50, message = "Cách gọi người hỗ trợ không được vượt quá 50 ký tự")
    private String caregiverTitle;
}
