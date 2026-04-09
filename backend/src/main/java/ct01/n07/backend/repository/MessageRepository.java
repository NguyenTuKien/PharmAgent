package ct01.n07.backend.repository;

import ct01.n07.backend.model.Message;
import ct01.n07.backend.model.enums.MessageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends MongoRepository<Message, String> {
    Page<Message> findByReceiverIdAndStatus(String receiverId, MessageStatus status, Pageable pageable);
}
