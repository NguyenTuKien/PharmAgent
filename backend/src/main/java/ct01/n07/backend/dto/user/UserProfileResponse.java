package ct01.n07.backend.dto.user;

import ct01.n07.backend.model.UserContact;
import ct01.n07.backend.model.enums.Gender;
import ct01.n07.backend.model.enums.Role;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

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
    private List<UserContact> userContacts;
    private Role role;
    private Instant createdAt;
    private Instant updatedAt;
}