package ct01.web.backend.repository;

import ct01.web.backend.model.DoseEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DoseEventRepository extends MongoRepository<DoseEvent, String> {
}
