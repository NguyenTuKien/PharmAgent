package ct01.n07.backend.controller;

import ct01.n07.backend.model.ChatMessage;
import ct01.n07.backend.model.ChatRoom;
import ct01.n07.backend.service.ChatMessageService;
import ct01.n07.backend.service.ChatRoomService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private static final String PROFILE_ID_ATTR = "profileId";

    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;

    /** Lấy profileId từ request attribute (được set bởi JwtAuthenticationFilter từ access token) */
    private String getCurrentProfileId(HttpServletRequest request) {
        String profileId = (String) request.getAttribute(PROFILE_ID_ATTR);
        if (profileId == null || profileId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Access token với profile là bắt buộc");
        }
        return profileId;
    }

    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoom>> getMyRooms(HttpServletRequest request) {
        String profileId = getCurrentProfileId(request);
        return ResponseEntity.ok(chatRoomService.getUserRooms(profileId));
    }

    @PostMapping("/rooms/direct/{targetProfileId}")
    public ResponseEntity<ChatRoom> getOrCreateDirectRoom(
            HttpServletRequest request,
            @PathVariable String targetProfileId) {

        String profileId = getCurrentProfileId(request);

        if (profileId.equals(targetProfileId)) {
            return ResponseEntity.badRequest().build();
        }

        ChatRoom room = chatRoomService.getOrCreateDirectRoom(profileId, targetProfileId);
        return ResponseEntity.ok(room);
    }

    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<Page<ChatMessage>> getRoomMessages(
            HttpServletRequest request,
            @PathVariable String roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        String profileId = getCurrentProfileId(request);

        if (!chatRoomService.isUserInRoom(roomId, profileId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(chatMessageService.getRoomMessages(roomId, pageable));
    }
}
