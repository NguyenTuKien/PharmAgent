package ct01.n07.backend.dto.auth;

import ct01.n07.backend.model.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserResponse {
    private String id;
    private String email;
    private UserStatus userStatus;
    private Instant createdAt;
    private Instant updatedAt;
}
