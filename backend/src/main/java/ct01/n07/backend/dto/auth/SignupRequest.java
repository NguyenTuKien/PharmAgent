package ct01.n07.backend.dto.auth;

import ct01.n07.backend.model.enums.Gender;
import ct01.n07.backend.model.enums.PermissionLevel;
import ct01.n07.backend.model.enums.Role;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignupRequest {
    // User information
    @NotBlank(message = "Email không được để trống")
    @Email(regexp = "^[a-zA-Z0-9+_.-]+@[a-zA-Z0-9.-]+$", message = "Email không đúng định dạng")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 8, message = "Mật khẩu phải có tối thiểu 8 ký tự")
    private String password;

    @NotBlank(message = "Xác nhận mật khẩu không được để trống")
    private String confirmPassword;

    // Caregiver information
    @NotBlank(message = "Tên không được để trống")
    @Size(max = 50, message = "Tên không được vượt quá 50 ký tự")
    private String caregiverFirstName;

    @NotBlank(message = "Họ không được để trống")
    @Size(max = 50, message = "Họ không được vượt quá 50 ký tự")
    private String caregiverLastName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(0|\\+84)[0-9]{9,10}$", message = "Số điện thoại không đúng định dạng (Ví dụ: 0912345678)")
    private String caregivePhone;

    @NotNull(message = "Ngày sinh không được để trống")
    @Past(message = "Ngày sinh phải là một ngày trong quá khứ")
    private LocalDate caregiverDateOfBirth;

    @NotNull(message = "Giới tính không được để trống")
    private Gender caregiverGender;

    @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự")
    private String caregiverAddress;

    @Pattern(regexp = "^(http|https)://.*$", message = "Avatar URL phải bắt đầu bằng http hoặc https")
    private String caregiverAvatarUrl;

    // Elderly information
    @NotBlank(message = "Tên không được để trống")
    @Size(max = 50, message = "Tên không được vượt quá 50 ký tự")
    private String elderlyFirstName;

    @NotBlank(message = "Họ không được để trống")
    @Size(max = 50, message = "Họ không được vượt quá 50 ký tự")
    private String elderlyLastName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(0|\\+84)[0-9]{9,10}$", message = "Số điện thoại không đúng định dạng (Ví dụ: 0912345678)")
    private String elderlyphone;

    @NotNull(message = "Ngày sinh không được để trống")
    @Past(message = "Ngày sinh phải là một ngày trong quá khứ")
    private LocalDate elderlyDateOfBirth;

    @NotNull(message = "Giới tính không được để trống")
    private Gender elderlyGender;

    @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự")
    private String elderlyAddress;

    @Pattern(regexp = "^(http|https)://.*$", message = "Avatar URL phải bắt đầu bằng http hoặc https")
    private String elderlyAvatarUrl;

    // Relationship information
    /**
     * Tên gợi nhớ cho người già (Không bắt buộc)
     */
    @Nullable // (Chỉ viết @Nullable không thôi, không có ngoặc)
    private String relationshipName;

    @NotNull(message = "Permission level is required")
    private PermissionLevel permissionLevel;
}
