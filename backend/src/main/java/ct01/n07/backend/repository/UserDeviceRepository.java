package ct01.n07.backend.repository;

import ct01.n07.backend.model.UserDevice;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserDeviceRepository extends MongoRepository<UserDevice, String> {

    List<UserDevice> findAllByUserId(String userId);

    Optional<UserDevice> findByDeviceToken(String deviceToken);

    Optional<UserDevice> findByDeviceTokenAndUserId(String deviceToken, String userId);

    Optional<UserDevice> findByIdAndUserId(String id, String userId);

    List<UserDevice> findAllByUserIdAndIsActive(String userId, boolean isActive);

    void deleteAllByUserId(String userId);
}
