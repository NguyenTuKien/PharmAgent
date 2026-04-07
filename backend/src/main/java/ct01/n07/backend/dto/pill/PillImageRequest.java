package ct01.n07.backend.dto.pill;

import ct01.n07.backend.model.enums.ViewType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PillImageRequest {
    @NotBlank(message = "URL ảnh không được để trống")
    @Size(max = 1000, message = "URL ảnh không được vượt quá 1000 ký tự")
    @Pattern(regexp = "^(http|https)://.*$", message = "URL ảnh phải bắt đầu bằng http hoặc https")
    private String imageUrl;

    @NotNull(message = "Loại góc nhìn (ViewType) không được để trống")
    private ViewType viewType;

    private boolean isPrimary;
}
