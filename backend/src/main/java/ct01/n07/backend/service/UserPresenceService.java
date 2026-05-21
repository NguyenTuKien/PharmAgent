package ct01.n07.backend.service;

import ct01.n07.backend.dto.session.SessionInfo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public interface UserPresenceService {
    void trackSession(String token, String tokenId, String userId, String profileId, String role,
                      String ipAddress, String userAgent, LocalDateTime expiresAt);

    List<SessionInfo> getAllActiveSessions();

    void removeSession(String tokenId);

    String getTokenByTokenId(String tokenId);

    void setOnline(String userId, String sessionId);

    void setOffline(String userId);

    boolean isUserOnline(String userId);

    Set<String> getOnlineUsers();

    String getSessionId(String userId);

    boolean forceLogout(String userId);
}
