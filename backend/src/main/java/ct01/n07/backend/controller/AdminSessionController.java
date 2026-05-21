package ct01.n07.backend.controller;

import ct01.n07.backend.dto.session.ActiveSessionsResponse;
import ct01.n07.backend.dto.session.SessionInfo;
import ct01.n07.backend.security.JwtService;
import ct01.n07.backend.service.UserPresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/admin/sessions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSessionController {

    private final UserPresenceService userPresenceService;
    private final JwtService jwtService;

    @GetMapping
    public ResponseEntity<ActiveSessionsResponse> getAllActiveSessions() {
        List<SessionInfo> sessions = userPresenceService.getAllActiveSessions();
        ActiveSessionsResponse response = ActiveSessionsResponse.builder()
                .totalSessions(sessions.size())
                .sessions(sessions)
                .build();
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{tokenId}")
    public ResponseEntity<Map<String, String>> revokeSession(@PathVariable String tokenId) {
        log.info("Admin revoking session: {}", tokenId);
        
        // Get the actual token
        String token = userPresenceService.getTokenByTokenId(tokenId);
        
        // Blacklist the token
        if (token != null) {
            jwtService.blacklistTokens(List.of(token));
            try {
                String userId = jwtService.extractUserId(token);
                if (userId != null) {
                    jwtService.blacklistUser(userId);
                }
            } catch (Exception e) {
                log.warn("Could not extract userId to blacklist user globally for session {}", tokenId);
            }
            log.info("Blacklisted token and user for session: {}", tokenId);
        }
        
        // Remove from active sessions
        userPresenceService.removeSession(tokenId);
        
        return ResponseEntity.ok(Map.of(
                "message", "Session revoked successfully",
                "tokenId", tokenId
        ));
    }

    @DeleteMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> revokeAllUserSessions(@PathVariable String userId) {
        log.info("Admin revoking all sessions for user: {}", userId);
        
        List<SessionInfo> allSessions = userPresenceService.getAllActiveSessions();
        List<String> tokensToBlacklist = allSessions.stream()
                .filter(session -> session.getUserId().equals(userId))
                .map(SessionInfo::getTokenId)
                .map(userPresenceService::getTokenByTokenId)
                .filter(token -> token != null)
                .toList();
        
        // Blacklist all tokens
        if (!tokensToBlacklist.isEmpty()) {
            jwtService.blacklistTokens(tokensToBlacklist);
        }
        
        // Cực kỳ quan trọng: Blacklist user ID để Refresh Token cũng bị vô hiệu hoá
        jwtService.blacklistUser(userId);
        
        // Remove sessions
        long revokedCount = allSessions.stream()
                .filter(session -> session.getUserId().equals(userId))
                .peek(session -> userPresenceService.removeSession(session.getTokenId()))
                .count();
        
        return ResponseEntity.ok(Map.of(
                "message", "All user sessions revoked",
                "userId", userId,
                "revokedCount", revokedCount
        ));
    }
}
