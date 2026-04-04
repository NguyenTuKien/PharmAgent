package ct01.web.backend.service;

import ct01.web.backend.dto.userProfile.CreateProfileRequest;
import ct01.web.backend.dto.userProfile.UpdateProfileRequest;
import ct01.web.backend.dto.userProfile.UserProfileResponse;
import ct01.web.backend.dto.userProfile.UserProfileSummaryResponse;
import ct01.web.backend.model.UserProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserProfileService {
    void saveUserProfile(UserProfile userProfile);

    UserProfile findById(String profileId);

    Page<UserProfile> findAllByUserId(String userId, Pageable pageable);

    String getCurrentUserId();

    UserProfile getCurrentUserProfile();

    Page<UserProfileSummaryResponse> getProfiles(Pageable pageable);

    UserProfileResponse createProfile(CreateProfileRequest request);

    void deleteProfile(String profileId);

    UserProfileResponse getMyProfile();

    UserProfileResponse updateMyProfile(UpdateProfileRequest request);
}
