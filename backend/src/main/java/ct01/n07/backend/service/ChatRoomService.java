package ct01.n07.backend.service;

import ct01.n07.backend.dto.chat.ChatRoomSummaryResponse;
import ct01.n07.backend.model.ChatRoom;
import java.util.List;

public interface ChatRoomService {
    ChatRoom getOrCreateDirectRoom(String currentUserId, String otherUserId);
    List<ChatRoom> getUserRooms(String userId);
    List<ChatRoomSummaryResponse> getUserRoomSummaries(String userId);
    ChatRoomSummaryResponse toRoomSummary(ChatRoom room, String currentProfileId);
    void updateLastMessage(String roomId, String lastMessageId);
    boolean isUserInRoom(String roomId, String userId);
}
