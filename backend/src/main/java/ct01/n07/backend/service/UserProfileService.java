package ct01.n07.backend.service;

import ct01.n07.backend.dto.userProfile.CreateProfileRequest;
import ct01.n07.backend.dto.userProfile.EmergencyContactRequest;
import ct01.n07.backend.dto.userProfile.UpdateProfileRequest;
import ct01.n07.backend.dto.userProfile.UserDeviceRequest;
import ct01.n07.backend.dto.userProfile.UserProfileResponse;
import ct01.n07.backend.dto.userProfile.UserProfileSummaryResponse;
import ct01.n07.backend.model.EmergencyContact;
import ct01.n07.backend.model.UserDevice;
import ct01.n07.backend.model.UserProfile;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

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

    boolean findByPhone(@NotBlank(message = "Số điện thoại không được để trống") @Pattern(regexp = "^(0|\\+84)[0-9]{9,10}$", message = "Số điện thoại không đúng định dạng (Ví dụ: 0912345678)") String phone);

    UserProfileResponse addContact(EmergencyContactRequest request);

    UserProfileResponse updateContact(String contactId, EmergencyContactRequest request);

    UserProfileResponse deleteContact(String contactId);

    List<EmergencyContact> getMyContacts();

    UserProfileResponse addDevice(UserDeviceRequest request);

    UserProfileResponse updateDevice(String deviceId, UserDeviceRequest request);

    UserProfileResponse deleteDevice(String deviceId);

    List<UserDevice> getMyDevices();
}
