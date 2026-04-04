package ct01.web.backend.dto.userProfile;

import ct01.web.backend.model.enums.Gender;
import ct01.web.backend.model.enums.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateProfileRequest {

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must be <= 100 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must be <= 100 characters")
    private String lastName;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^\\+?[0-9]{9,15}$", message = "Phone format is invalid")
    private String phone;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @NotNull(message = "Gender is required")
    private Gender gender;

    @Size(max = 255, message = "Address must be <= 255 characters")
    private String address;

    @Size(max = 500, message = "Avatar URL must be <= 500 characters")
    private String avatarUrl;

    @NotNull(message = "Role is required")
    private Role role;
}