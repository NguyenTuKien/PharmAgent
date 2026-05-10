package ct01.n07.backend.repository;

import ct01.n07.backend.model.ChatRoom;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends MongoRepository<ChatRoom, String> {
    List<ChatRoom> findByParticipantIdsContaining(String userId);
    
    // Tìm phòng direct chat giữa 2 user (Sử dụng $all để tránh lỗi xung đột key participantIds)
    @Query("{ 'type': ?0, 'participantIds': { '$all': [?1, ?2] } }")
    Optional<ChatRoom> findByTypeAndParticipantIdsContainingAndParticipantIdsContaining(String type, String user1Id, String user2Id);
}
