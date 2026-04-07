package ct01.n07.backend.dto.userProfile;

import ct01.n07.backend.model.enums.Gender;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateProfileRequest {

    @Size(max = 100, message = "First name must be <= 100 characters")
    private String firstName;

    @Size(max = 100, message = "Last name must be <= 100 characters")
    private String lastName;

    @Pattern(regexp = "^\\+?[0-9]{9,15}$", message = "Phone format is invalid")
    private String phone;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    private Gender gender;

    @Size(max = 255, message = "Address must be <= 255 characters")
    private String address;

    @Size(max = 500, message = "Avatar URL must be <= 500 characters")
    private String avatarUrl;
}