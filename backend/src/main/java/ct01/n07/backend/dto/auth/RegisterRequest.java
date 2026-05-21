package ct01.n07.backend.dto.auth;

import ct01.n07.backend.model.enums.Gender;
import ct01.n07.backend.model.enums.PermissionLevel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    // User information
    @NotBlank(message = "Email không được để trống")
    @Email(regexp = "^[a-zA-Z0-9+_.-]+@[a-zA-Z0-9.-]+$", message = "Email không đúng định dạng")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 8, message = "Mật khẩu phải có tối thiểu 8 ký tự")
    private String password;

    @NotBlank(message = "Xác nhận mật khẩu không được để trống")
    private String confirmPassword;

    @NotNull(message = "Caregiver information is required")
    @Valid
    private CaregiverRegisterRequest caregiver;

    @Valid
    private ElderlyRegisterRequest elderly;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CaregiverRegisterRequest {
        @NotBlank(message = "Tên không được để trống")
        @Size(max = 50, message = "Tên không được vượt quá 50 ký tự")
        private String firstName;

        @Size(max = 50, message = "Họ không được vượt quá 50 ký tự")
        private String lastName;

        @Pattern(regexp = "^(0|\\+84)[0-9]{9,10}$", message = "Số điện thoại không đúng định dạng (Ví dụ: 0912345678)")
        private String phone;

        @Past(message = "Ngày sinh phải là một ngày trong quá khứ")
        private LocalDate dateOfBirth;

        private Gender gender;

        @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự")
        private String address;

        @Pattern(regexp = "^(http|https)://.*$", message = "Avatar URL phải bắt đầu bằng http hoặc https")
        private String avatarUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ElderlyRegisterRequest {
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

        @NotNull(message = "Permission level is required")
        private PermissionLevel permissionLevel;
    }
}
