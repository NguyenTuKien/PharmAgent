package ct01.n07.backend.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateAvatarRequest {

    @NotBlank(message = "Avatar URL không được để trống")
    @Size(max = 1000, message = "Avatar URL không được vượt quá 1000 ký tự")
    @Pattern(regexp = "^(http|https)://.*$", message = "Avatar URL phải bắt đầu bằng http hoặc https")
    private String avatarUrl;
}
