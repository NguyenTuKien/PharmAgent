package ct01.n07.backend.dto.auth;

import ct01.n07.backend.model.enums.Gender;
import ct01.n07.backend.model.enums.Role;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class UserInfoResponse {
    String userId;
    String phone;
    String firstName;
    String lastName;
    String email;
    Gender gender;
    LocalDate dateOfBirth;
    String address;
    String avatarUrl;
    Role role;
}
