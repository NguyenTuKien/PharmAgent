package ct01.web.backend.dto.userProfile;

import ct01.web.backend.model.enums.Role;
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