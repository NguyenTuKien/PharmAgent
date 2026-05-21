package ct01.n07.backend.dto.auth;

import ct01.n07.backend.model.enums.FamilyRelation;
import ct01.n07.backend.model.enums.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RegisterElderlyRequest {
    @NotBlank(message = "Tên không được để trống")
    @Size(max = 50, message = "Tên không được vượt quá 50 ký tự")
    private String firstName;

    @NotBlank(message = "Họ không được để trống")
    @Size(max = 50, message = "Họ không được vượt quá 50 ký tự")
    private String lastName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(0|\\+84)[0-9]{9,10}$", message = "Số điện thoại không đúng định dạng (Ví dụ: 0912345678)")
    private String phone;

    @NotNull(message = "Ngày sinh không được để trống")
    @Past(message = "Ngày sinh phải là một ngày trong quá khứ")
    private LocalDate dateOfBirth;

    @NotNull(message = "Giới tính không được để trống")
    private Gender gender;

    @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự")
    private String address;

    @Pattern(regexp = "^(http|https)://.*$", message = "Avatar URL phải bắt đầu bằng http hoặc https")
    private String avatarUrl;

    private String caregiverTitle;
    private String elderlyTitle;

    @NotNull(message = "Quan hệ không được để trống")
    private FamilyRelation relation;

    @Size(max = 50, message = "Quan hệ khác không được vượt quá 50 ký tự")
    private String customRelation;
}
