package ct01.n07.backend.repository;

import ct01.n07.backend.model.ChatRoom;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends MongoRepository<ChatRoom, String> {
    List<ChatRoom> findByParticipantIdsContaining(String userId);
    
    // Tìm phòng direct chat giữa 2 user
    Optional<ChatRoom> findByTypeAndParticipantIdsContainingAndParticipantIdsContaining(String type, String user1Id, String user2Id);
}
