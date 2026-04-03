package ct01.web.backend.dto.auth;

import ct01.web.backend.model.enums.Gender;
import ct01.web.backend.model.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignupRequest {
    private String email;
    private String password;
    private String confirmPassword;

    private String firstName;
    private String lastName;
    private String phone;

    private LocalDate dateOfBirth;
    private Gender gender;
    private String address;
    private String avatarUrl;

    private Role role;
}
