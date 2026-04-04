package ct01.web.backend.dto.userProfile;

import ct01.web.backend.model.enums.Gender;
import ct01.web.backend.model.enums.Role;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

@Data
public class UserProfileResponse {
    private String id;
    private String userId;
    private String firstName;
    private String lastName;
    private String phone;
    private LocalDate dateOfBirth;
    private Gender gender;
    private String address;
    private String avatarUrl;
    private Role role;
    private Instant createdAt;
    private Instant updatedAt;
}