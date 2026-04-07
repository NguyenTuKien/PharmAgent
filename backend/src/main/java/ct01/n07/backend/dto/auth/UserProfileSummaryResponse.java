package ct01.n07.backend.dto.auth;

import ct01.n07.backend.model.enums.Role;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserProfileSummaryResponse {
    private String id;
    private String userId;
    private String phone;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private Role role;
}

