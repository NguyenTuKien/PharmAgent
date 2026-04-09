package ct01.n07.backend.service;

import ct01.n07.backend.dto.userProfile.CreateProfileRequest;
import ct01.n07.backend.dto.userProfile.UpdateProfileRequest;
import ct01.n07.backend.dto.userProfile.UserProfileResponse;
import ct01.n07.backend.dto.userProfile.UserProfileSummaryResponse;
import ct01.n07.backend.model.UserProfile;
import ct01.n07.backend.model.enums.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
public interface UserProfileService {
    void saveUserProfile(UserProfile userProfile);

    UserProfile findById(String profileId);
    
    List<UserProfile> findAllById(List<String> ids);

    Page<UserProfile> findAllByUserId(String userId, Pageable pageable);

    String getCurrentUserId();

    UserProfile getCurrentUserProfile();

    Page<UserProfileSummaryResponse> getProfiles(Pageable pageable);

    UserProfileResponse createProfile(CreateProfileRequest request);

    void deleteProfile(String profileId);

    UserProfileResponse getMyProfile();

    UserProfileResponse updateMyProfile(UpdateProfileRequest request);

    boolean findByPhone(@NotBlank(message = "Số điện thoại không được để trống") @Pattern(regexp = "^(0|\\+84)[0-9]{9,10}$", message = "Số điện thoại không đúng định dạng (Ví dụ: 0912345678)") String phone);



    Page<UserProfileSummaryResponse> searchProfiles(String keyword, Role role, Pageable pageable);
}
