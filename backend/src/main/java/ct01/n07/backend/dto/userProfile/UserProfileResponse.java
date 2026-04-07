package ct01.n07.backend.dto.userProfile;

import ct01.n07.backend.model.EmergencyContact;
import ct01.n07.backend.model.UserDevice;
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
    private List<EmergencyContact> emergencyContacts;
    private List<UserDevice> userDevices;
    private Role role;
    private Instant createdAt;
    private Instant updatedAt;
}