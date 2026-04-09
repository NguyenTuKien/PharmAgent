package ct01.n07.backend.service.impl;

import ct01.n07.backend.constant.ProfileConstant;
import ct01.n07.backend.dto.userProfile.CreateProfileRequest;
import ct01.n07.backend.dto.userProfile.UserProfileResponse;
import ct01.n07.backend.dto.userProfile.UserProfileSummaryResponse;
import ct01.n07.backend.dto.userProfile.UpdateProfileRequest;
import ct01.n07.backend.exception.CannotDeleteActiveProfileException;
import ct01.n07.backend.mapper.UserProfileMapper;
import ct01.n07.backend.model.UserProfile;
import ct01.n07.backend.model.enums.Role;
import ct01.n07.backend.repository.UserProfileRepository;
import ct01.n07.backend.service.SecurityContextService;
import ct01.n07.backend.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;


@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final UserProfileMapper userProfileMapper;
    private final SecurityContextService securityContextService;

    @Override
    public void saveUserProfile(UserProfile userProfile) {
        userProfileRepository.save(userProfile);
    }

    @Override
    public UserProfile findById(String profileId) {
        return userProfileRepository.findById(profileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ProfileConstant.PROFILE_NOT_FOUND));
    }

    @Override
    public List<UserProfile> findAllById(List<String> ids) {
        return userProfileRepository.findAllById(ids);
    }

    @Override
    public Page<UserProfile> findAllByUserId(String userId, Pageable pageable) {
        return userProfileRepository.findAllByUserId(userId, pageable);
    }

    @Override
    public String getCurrentUserId() {
        return securityContextService.getCurrentUserId();
    }

    @Override
    public UserProfile getCurrentUserProfile() {
        String userId = getCurrentUserId();

        String profileId = securityContextService.getCurrentProfileId();
        if (profileId == null || profileId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, ProfileConstant.PROFILE_TOKEN_REQUIRED);
        }

        UserProfile profile = findById(profileId);
        if (!userId.equals(profile.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ProfileConstant.PROFILE_NOT_BELONG_TO_USER);
        }
        return profile;
    }

    @Override
    public Page<UserProfileSummaryResponse> getProfiles(Pageable pageable) {
        String userId = getCurrentUserId();
        String currentProfileId = securityContextService.getCurrentProfileId(); // null nếu dùng accessToken

        if (currentProfileId != null && !currentProfileId.isBlank()) {
            return userProfileRepository.findAllByUserIdAndIdNot(userId, currentProfileId, pageable)
                    .map(userProfileMapper::toProfileSummary);
        }

        return userProfileRepository.findAllByUserId(userId, pageable)
                .map(userProfileMapper::toProfileSummary);
    }

    @Override
    public UserProfileResponse createProfile(CreateProfileRequest request) {
        String userId = getCurrentUserId();

        if (userProfileRepository.existsByPhone(request.getPhone())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ProfileConstant.PHONE_ALREADY_EXISTS);
        }

        UserProfile userProfile = userProfileMapper.toUserProfile(request);
        userProfile.setUserId(userId);

        return userProfileMapper.toResponse(userProfileRepository.save(userProfile));
    }

    @Override
    public void deleteProfile(String profileId) {
        String userId = getCurrentUserId();
        String currentProfileId = securityContextService.getCurrentProfileId();

        // Không cho phép xóa profile đang được sử dụng
        if (profileId.equals(currentProfileId)) {
            throw new CannotDeleteActiveProfileException();
        }

        UserProfile profile = userProfileRepository.findByIdAndUserId(profileId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ProfileConstant.PROFILE_NOT_FOUND));
        userProfileRepository.delete(profile);
    }

    @Override
    public UserProfileResponse getMyProfile() {
        return userProfileMapper.toResponse(getCurrentUserProfile());
    }

    @Override
    public UserProfileResponse updateMyProfile(UpdateProfileRequest request) {
        UserProfile currentProfile = getCurrentUserProfile();

        if (request.getPhone() != null
                && !request.getPhone().equals(currentProfile.getPhone())
                && userProfileRepository.existsByPhoneAndIdNot(request.getPhone(), currentProfile.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ProfileConstant.PHONE_ALREADY_EXISTS);
        }

        userProfileMapper.updateUserProfile(request, currentProfile);
        return userProfileMapper.toResponse(userProfileRepository.save(currentProfile));
    }



    @Override
    public boolean findByPhone(String phone) {
        return userProfileRepository.existsByPhone(phone);
    }



    @Override
    public Page<UserProfileSummaryResponse> searchProfiles(String keyword, Role role, Pageable pageable) {
        String currentProfileId = securityContextService.getCurrentProfileId();
        String excludedId = currentProfileId != null ? currentProfileId : "";

        Page<UserProfile> profilePage;
        if (role == null) {
            profilePage = userProfileRepository.searchProfilesExcludingCurrent(keyword, excludedId, pageable);
        } else {
            profilePage = userProfileRepository.searchProfilesByRoleExcludingCurrent(keyword, excludedId, String.valueOf(role), pageable);
        }

        return profilePage.map(userProfileMapper::toProfileSummary);
    }
}