package ct01.web.backend.service;

import ct01.web.backend.model.UserProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserProfileService {
    void saveUserProfile(UserProfile userProfile);

    UserProfile findById(String profileId);

    Page<UserProfile> findAllByUserId(String userId, Pageable pageable);

    UserProfile getCurrentUserProfile();
}
