package ct01.n07.backend.dto.pill;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PillRequest {

    @NotBlank(message = "Tên thuốc không được để trống")
    @Size(max = 150, message = "Tên thuốc không được vượt quá 150 ký tự")
    private String name;

    @NotBlank(message = "Tên gốc/hoạt chất không được để trống")
    @Size(max = 150, message = "Tên gốc/hoạt chất không được vượt quá 150 ký tự")
    private String genericName;

    @Size(max = 150, message = "Tên thương hiệu không được vượt quá 150 ký tự")
    private String brandName;

    @NotBlank(message = "Hàm lượng không được để trống")
    @Size(max = 50, message = "Hàm lượng không được vượt quá 50 ký tự (VD: 500mg)")
    private String strength;

    @NotBlank(message = "Dạng bào chế không được để trống")
    @Size(max = 50, message = "Dạng bào chế không được vượt quá 50 ký tự (VD: Viên nén, Viên nang)")
    private String dosageForm;

    @Size(max = 50, message = "Màu sắc không được vượt quá 50 ký tự")
    private String color;

    @Size(max = 50, message = "Hình dáng không được vượt quá 50 ký tự")
    private String shape;

    @Size(max = 1000, message = "Mô tả không được vượt quá 1000 ký tự")
    private String description;

    @NotBlank(message = "Hướng dẫn sử dụng không được để trống")
    @Size(max = 1000, message = "Hướng dẫn sử dụng không được vượt quá 1000 ký tự")
    private String usageInstructions;

    @Size(max = 1000, message = "Cảnh báo không được vượt quá 1000 ký tự")
    private String warning;

    @Size(max = 1000, message = "Tác dụng phụ không được vượt quá 1000 ký tự")
    private String sideEffects;

    @NotBlank(message = "Nhà sản xuất không được để trống")
    @Size(max = 150, message = "Tên nhà sản xuất không được vượt quá 150 ký tự")
    private String manufacturer;
}