package ct01.n07.backend.repository;

import ct01.n07.backend.model.CallLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CallLogRepository extends MongoRepository<CallLog, String> {
    List<CallLog> findByCallerIdOrReceiverId(String callerId, String receiverId);
}
