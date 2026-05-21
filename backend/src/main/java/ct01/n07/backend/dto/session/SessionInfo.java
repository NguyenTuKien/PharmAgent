package ct01.n07.backend.dto.session;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionInfo {
    private String tokenId;           // JTI (JWT ID) hoặc hash của token
    private String userId;
    private String profileId;
    private String role;
    private String userEmail;
    private String profileName;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime loginAt;
    private LocalDateTime expiresAt;
}
