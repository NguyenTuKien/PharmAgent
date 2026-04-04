package ct01.web.backend.service.impl;

import ct01.web.backend.constant.ProfileConstant;
import ct01.web.backend.dto.userProfile.CreateProfileRequest;
import ct01.web.backend.dto.userProfile.UpdateProfileRequest;
import ct01.web.backend.dto.userProfile.UserProfileResponse;
import ct01.web.backend.dto.userProfile.UserProfileSummaryResponse;
import ct01.web.backend.mapper.UserProfileMapper;
import ct01.web.backend.model.UserProfile;
import ct01.web.backend.repository.UserProfileRepository;
import ct01.web.backend.service.UserProfileService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private static final String PROFILE_ID_ATTR = "profileId";

    private final UserProfileRepository userProfileRepository;
    private final UserProfileMapper userProfileMapper;

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
    public Page<UserProfile> findAllByUserId(String userId, Pageable pageable) {
        return userProfileRepository.findAllByUserId(userId, pageable);
    }

    @Override
    public String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, ProfileConstant.UNAUTHORIZED);
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof String userId) || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, ProfileConstant.INVALID_AUTHENTICATION_PRINCIPAL);
        }

        String profileId = getProfileIdFromRequest();
        if (profileId != null && !profileId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, ProfileConstant.ACCESS_TOKEN_REQUIRED);
        }

        return userId;
    }

    @Override
    public UserProfile getCurrentUserProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, ProfileConstant.UNAUTHORIZED);
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof String userId) || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, ProfileConstant.INVALID_AUTHENTICATION_PRINCIPAL);
        }

        String profileId = getProfileIdFromRequest();
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

    private String getProfileIdFromRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;
        return request != null ? (String) request.getAttribute(PROFILE_ID_ATTR) : null;
    }
}