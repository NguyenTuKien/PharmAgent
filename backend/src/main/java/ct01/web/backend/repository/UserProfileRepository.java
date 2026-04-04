package ct01.web.backend.repository;

import ct01.web.backend.model.UserProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserProfileRepository extends MongoRepository<UserProfile, String> {
    Page<UserProfile> findAllByUserId(String userId, Pageable pageable);

    Optional<UserProfile> findByIdAndUserId(String id, String userId);

    boolean existsByPhone(String phone);

    boolean existsByPhoneAndIdNot(String phone, String id);
}
