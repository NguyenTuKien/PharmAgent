package ct01.n07.backend.controller;

import ct01.n07.backend.dto.chat.ChatPayload;
import ct01.n07.backend.dto.chat.ChatRoomSummaryResponse;
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
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
    private final SimpMessagingTemplate messagingTemplate;

    private String getCurrentProfileId(HttpServletRequest request) {
        String profileId = (String) request.getAttribute(PROFILE_ID_ATTR);
        if (profileId == null || profileId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Access token với profile là bắt buộc");
        }
        return profileId;
    }

    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoomSummaryResponse>> getMyRooms(HttpServletRequest request) {
        String profileId = getCurrentProfileId(request);
        return ResponseEntity.ok(chatRoomService.getUserRoomSummaries(profileId));
    }

    @PostMapping("/rooms/direct/{targetProfileId}")
    public ResponseEntity<ChatRoomSummaryResponse> getOrCreateDirectRoom(
            HttpServletRequest request,
            @PathVariable String targetProfileId) {

        String profileId = getCurrentProfileId(request);

        if (profileId.equals(targetProfileId)) {
            return ResponseEntity.badRequest().build();
        }

        ChatRoom room = chatRoomService.getOrCreateDirectRoom(profileId, targetProfileId);
        return ResponseEntity.ok(chatRoomService.toRoomSummary(room, profileId));
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

    @PostMapping("/rooms/{roomId}/read")
    public ResponseEntity<Void> markRoomAsRead(HttpServletRequest request, @PathVariable String roomId) {
        String profileId = getCurrentProfileId(request);
        if (!chatRoomService.isUserInRoom(roomId, profileId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        chatMessageService.markRoomAsRead(roomId, profileId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/rooms/{roomId}/messages")
    public ResponseEntity<ChatMessage> sendMessage(
            HttpServletRequest request,
            @PathVariable String roomId,
            @RequestBody ChatPayload payload) {

        String profileId = getCurrentProfileId(request);

        if (!chatRoomService.isUserInRoom(roomId, profileId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        payload.setRoomId(roomId);
        payload.setSenderId(profileId);

        ChatMessage saved = chatMessageService.saveMessage(payload);

        // Broadcast to WebSockets if peer is listening in real time
        try {
            messagingTemplate.convertAndSend("/topic/room." + roomId, saved);
        } catch (Exception e) {
            // Ignore broadcast failures during offline mode
        }

        return ResponseEntity.ok(saved);
    }
}
