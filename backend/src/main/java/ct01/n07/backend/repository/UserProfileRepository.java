package ct01.n07.backend.repository;

import ct01.n07.backend.model.UserProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import ct01.n07.backend.model.enums.Role;

@Repository
public interface UserProfileRepository extends MongoRepository<UserProfile, String> {
    Page<UserProfile> findAllByUserId(String userId, Pageable pageable);

    void deleteAllByUserId(String userId);

    Optional<UserProfile> findByIdAndUserId(String id, String userId);

    boolean existsByPhone(String phone);

    boolean existsByPhoneAndIdNot(String phone, String id);

    boolean existsByIdAndRole(String id, Role role);

    // Đổi tên hàm cho tổng quát hơn
    @Query("{ 'id': { $ne: ?1 }, 'role': ?2, $or: [ " +
            "{ 'firstName': { $regex: ?0, $options: 'i' } }, " +
            "{ 'lastName': { $regex: ?0, $options: 'i' } }, " +
            "{ 'phone': { $regex: ?0, $options: 'i' } } ] }")
    Page<UserProfile> searchProfilesByRoleExcludingCurrent(
            String keyword,
            String excludedProfileId,
            String role,
            Pageable pageable);

    @Query("{ 'id': { $ne: ?1 }, $or: [ " +
            "{ 'firstName': { $regex: ?0, $options: 'i' } }, " +
            "{ 'lastName': { $regex: ?0, $options: 'i' } }, " +
            "{ 'phone': { $regex: ?0, $options: 'i' } } ] }")
    Page<UserProfile> searchProfilesExcludingCurrent(
            String keyword,
            String excludedProfileId,
            Pageable pageable);
}
