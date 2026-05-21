package ct01.n07.backend.repository;

import ct01.n07.backend.model.Notification;
import ct01.n07.backend.model.enums.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {
    Page<Notification> findByReceiverIdAndStatus(String receiverId, NotificationStatus status, Pageable pageable);
    Page<Notification> findBySenderIdAndStatus(String senderId, NotificationStatus status, Pageable pageable);
}
