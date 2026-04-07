package ct01.n07.backend.dto.userProfile;

import ct01.n07.backend.model.enums.Role;
import lombok.Data;

@Data
public class UserProfileSummaryResponse {
    private String id;
    private String firstName;
    private String lastName;
    private String phone;
    private String avatarUrl;
    private Role role;
}