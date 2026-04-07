package ct01.n07.backend.dto.pill;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

@Data
public class PillCreateRequest {
    @NotBlank(message = "Tên thuốc không được để trống")
    @Size(max = 150, message = "Tên thuốc không được vượt quá 150 ký tự")
    private String name;

    @Size(max = 1000, message = "Mô tả không được vượt quá 1000 ký tự")
    private String description;

    @Size(max = 10, message = "Chỉ được gửi tối đa 10 hình ảnh")
    private List<@NotBlank(message = "URL hình ảnh không được để trống") @Size(max = 1000, message = "URL hình ảnh không được vượt quá 1000 ký tự") String> images;
}
