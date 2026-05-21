package ct01.n07.backend.repository;

import ct01.n07.backend.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    boolean existsById(String id);

    Optional<User> findByEmail(String email);

    Optional<User> findByGoogleSubject(String googleSubject);
}
