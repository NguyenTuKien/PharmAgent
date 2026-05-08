package ct01.n07.backend.service;

public interface UserPresenceService {
    void setOnline(String userId, String sessionId);
    void setOffline(String userId);
    boolean isUserOnline(String userId);
}
