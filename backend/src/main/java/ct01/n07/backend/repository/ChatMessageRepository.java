package ct01.n07.backend.repository;

import ct01.n07.backend.model.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {
    Page<ChatMessage> findByRoomIdOrderBySentAtDesc(String roomId, Pageable pageable);
    
    List<ChatMessage> findByRoomIdOrderBySentAtAsc(String roomId);

    Optional<ChatMessage> findFirstByRoomIdOrderBySentAtDesc(String roomId);

    @Query("{ 'roomId': ?0, 'senderId': { '$ne': ?1 }, 'readBy': { '$ne': ?2 } }")
    List<ChatMessage> findUnreadMessages(String roomId, String senderId, String profileId);
}
