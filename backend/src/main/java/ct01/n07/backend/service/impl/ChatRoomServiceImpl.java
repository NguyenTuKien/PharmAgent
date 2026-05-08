package ct01.n07.backend.service.impl;

import ct01.n07.backend.model.ChatRoom;
import ct01.n07.backend.model.enums.ChatRoomType;
import ct01.n07.backend.repository.ChatRoomRepository;
import ct01.n07.backend.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatRoomServiceImpl implements ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;

    @Override
    public ChatRoom getOrCreateDirectRoom(String currentUserId, String otherUserId) {
        Optional<ChatRoom> existingRoom = chatRoomRepository
                .findByTypeAndParticipantIdsContainingAndParticipantIdsContaining(
                        ChatRoomType.DIRECT.name(), currentUserId, otherUserId);

        if (existingRoom.isPresent()) {
            return existingRoom.get();
        }

        ChatRoom newRoom = ChatRoom.builder()
                .type(ChatRoomType.DIRECT.name())
                .participantIds(Arrays.asList(currentUserId, otherUserId))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return chatRoomRepository.save(newRoom);
    }

    @Override
    public List<ChatRoom> getUserRooms(String userId) {
        return chatRoomRepository.findByParticipantIdsContaining(userId);
    }

    @Override
    public void updateLastMessage(String roomId, String lastMessageId) {
        chatRoomRepository.findById(roomId).ifPresent(room -> {
            room.setLastMessageId(lastMessageId);
            room.setUpdatedAt(Instant.now());
            chatRoomRepository.save(room);
        });
    }

    @Override
    public boolean isUserInRoom(String roomId, String userId) {
        return chatRoomRepository.findById(roomId)
                .map(room -> room.getParticipantIds().contains(userId))
                .orElse(false);
    }
}

