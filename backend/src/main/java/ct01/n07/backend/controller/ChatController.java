package ct01.n07.backend.controller;

import ct01.n07.backend.model.ChatMessage;
import ct01.n07.backend.model.ChatRoom;
import ct01.n07.backend.model.User;
import ct01.n07.backend.service.ChatMessageService;
import ct01.n07.backend.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;

    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoom>> getMyRooms(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(chatRoomService.getUserRooms(currentUser.getId()));
    }

    @PostMapping("/rooms/direct/{targetUserId}")
    public ResponseEntity<ChatRoom> getOrCreateDirectRoom(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String targetUserId) {
        
        // Tránh tự chat với chính mình
        if (currentUser.getId().equals(targetUserId)) {
            return ResponseEntity.badRequest().build();
        }

        ChatRoom room = chatRoomService.getOrCreateDirectRoom(currentUser.getId(), targetUserId);
        return ResponseEntity.ok(room);
    }

    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<Page<ChatMessage>> getRoomMessages(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        // Kiểm tra quyền access phòng
        if (!chatRoomService.isUserInRoom(roomId, currentUser.getId())) {
            return ResponseEntity.status(403).build();
        }

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(chatMessageService.getRoomMessages(roomId, pageable));
    }
}
