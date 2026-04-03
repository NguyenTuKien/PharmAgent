package ct01.web.backend.repository;

import ct01.web.backend.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    boolean existsById(String id);

    Optional<User> findByEmail(String email);
}
