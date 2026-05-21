package ct01.n07.backend.service.impl;

import ct01.n07.backend.dto.chat.ChatParticipantResponse;
import ct01.n07.backend.dto.chat.ChatRoomSummaryResponse;
import ct01.n07.backend.model.ChatMessage;
import ct01.n07.backend.model.ChatRoom;
import ct01.n07.backend.model.UserProfile;
import ct01.n07.backend.model.enums.ChatRoomType;
import ct01.n07.backend.repository.ChatMessageRepository;
import ct01.n07.backend.repository.ChatRoomRepository;
import ct01.n07.backend.repository.UserProfileRepository;
import ct01.n07.backend.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatRoomServiceImpl implements ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserProfileRepository userProfileRepository;

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
    public List<ChatRoomSummaryResponse> getUserRoomSummaries(String userId) {
        return getUserRooms(userId).stream()
                .map(room -> toRoomSummary(room, userId))
                .sorted(Comparator.comparing(this::roomActivityAt).reversed())
                .toList();
    }

    @Override
    public ChatRoomSummaryResponse toRoomSummary(ChatRoom room, String currentProfileId) {
        Map<String, UserProfile> profileById = new HashMap<>();
        userProfileRepository.findAllById(room.getParticipantIds())
                .forEach(profile -> profileById.put(profile.getId(), profile));

        List<ChatParticipantResponse> participants = room.getParticipantIds().stream()
                .map(profileId -> toParticipant(profileById.get(profileId), profileId))
                .toList();

        ChatParticipantResponse peerProfile = participants.stream()
                .filter(participant -> !participant.getProfileId().equals(currentProfileId))
                .findFirst()
                .orElse(null);

        ChatMessage lastMessage = null;
        if (room.getLastMessageId() != null) {
            lastMessage = chatMessageRepository.findById(room.getLastMessageId()).orElse(null);
        }
        if (lastMessage == null) {
            lastMessage = chatMessageRepository.findFirstByRoomIdOrderBySentAtDesc(room.getId()).orElse(null);
        }

        long unreadCount = chatMessageRepository
                .findUnreadMessages(room.getId(), currentProfileId, currentProfileId)
                .size();

        return ChatRoomSummaryResponse.builder()
                .id(room.getId())
                .type(room.getType())
                .participantIds(room.getParticipantIds())
                .participants(participants)
                .peerProfile(peerProfile)
                .lastMessage(lastMessage)
                .unreadCount(unreadCount)
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                .build();
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

    private ChatParticipantResponse toParticipant(UserProfile profile, String fallbackProfileId) {
        if (profile == null) {
            return ChatParticipantResponse.builder()
                    .profileId(fallbackProfileId)
                    .firstName("Người dùng")
                    .lastName("")
                    .build();
        }

        return ChatParticipantResponse.builder()
                .profileId(profile.getId())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .phone(profile.getPhone())
                .avatarUrl(profile.getAvatarUrl())
                .role(profile.getRole() != null ? profile.getRole().name() : null)
                .build();
    }

    private Instant roomActivityAt(ChatRoomSummaryResponse room) {
        if (room.getLastMessage() != null && room.getLastMessage().getSentAt() != null) {
            return room.getLastMessage().getSentAt();
        }
        if (room.getUpdatedAt() != null) {
            return room.getUpdatedAt();
        }
        if (room.getCreatedAt() != null) {
            return room.getCreatedAt();
        }
        return Instant.EPOCH;
    }
}

