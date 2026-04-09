package ct01.n07.backend.service.impl;

import ct01.n07.backend.constant.ProfileConstant;
import ct01.n07.backend.dto.userProfile.CreateProfileRequest;
import ct01.n07.backend.dto.userProfile.EmergencyContactRequest;
import ct01.n07.backend.dto.userProfile.UpdateProfileRequest;
import ct01.n07.backend.dto.userProfile.UserProfileResponse;
import ct01.n07.backend.dto.userProfile.UserProfileSummaryResponse;
import ct01.n07.backend.dto.userProfile.UserDeviceRequest;
import ct01.n07.backend.exception.CannotDeleteActiveProfileException;
import ct01.n07.backend.mapper.UserProfileMapper;
import ct01.n07.backend.model.EmergencyContact;
import ct01.n07.backend.model.UserDevice;
import ct01.n07.backend.model.UserProfile;
import ct01.n07.backend.model.enums.Role;
import ct01.n07.backend.repository.UserProfileRepository;
import ct01.n07.backend.service.UserProfileService;
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

import java.util.ArrayList;
import java.util.List;

import static ct01.n07.backend.model.enums.Role.ELDERLY;

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
        String currentProfileId = getProfileIdFromRequest(); // null nếu dùng accessToken

        Page<UserProfile> all = userProfileRepository.findAllByUserId(userId, pageable);

        List<UserProfileSummaryResponse> filtered = all.stream()
                .filter(profile -> !profile.getId().equals(currentProfileId))
                .map(userProfileMapper::toProfileSummary)
                .toList();

        return new org.springframework.data.domain.PageImpl<>(filtered, pageable, filtered.size());
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
        String currentProfileId = getProfileIdFromRequest();

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

    private String getProfileIdFromRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;
        return request != null ? (String) request.getAttribute(PROFILE_ID_ATTR) : null;
    }

    @Override
    public boolean findByPhone(String phone) {
        return userProfileRepository.existsByPhone(phone);
    }

    @Override
    public UserProfileResponse addContact(EmergencyContactRequest request) {
        UserProfile profile = getCurrentUserProfile();
        if (profile.getEmergencyContacts() == null) {
            profile.setEmergencyContacts(new ArrayList<>());
        }

        // Kiểm tra nếu số điện thoại đã tồn tại thì cập nhật tên
        java.util.Optional<EmergencyContact> existingContact = profile.getEmergencyContacts().stream()
                .filter(c -> c.getPhone().equals(request.getPhone()))
                .findFirst();

        if (existingContact.isPresent()) {
            existingContact.get().setName(request.getName());
        } else {
            // Nếu chưa có thì thêm mới
            EmergencyContact contact = EmergencyContact.builder()
                    .name(request.getName())
                    .phone(request.getPhone())
                    .build();
            profile.getEmergencyContacts().add(contact);
        }

        return userProfileMapper.toResponse(userProfileRepository.save(profile));
    }

    @Override
    public UserProfileResponse updateContact(String contactId, EmergencyContactRequest request) {
        UserProfile profile = getCurrentUserProfile();
        List<EmergencyContact> contacts = profile.getEmergencyContacts();
        if (contacts == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contact list is empty");
        }
        EmergencyContact contactToUpdate = contacts.stream()
                .filter(c -> c.getId().equals(contactId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contact not found"));

        contactToUpdate.setName(request.getName());
        contactToUpdate.setPhone(request.getPhone());

        return userProfileMapper.toResponse(userProfileRepository.save(profile));
    }

    @Override
    public UserProfileResponse deleteContact(String contactId) {
        UserProfile profile = getCurrentUserProfile();
        List<EmergencyContact> contacts = profile.getEmergencyContacts();
        if (contacts == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contact list is empty");
        }
        boolean removed = contacts.removeIf(c -> c.getId().equals(contactId));
        if (!removed) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contact not found");
        }
        return userProfileMapper.toResponse(userProfileRepository.save(profile));
    }

    @Override
    public List<EmergencyContact> getMyContacts() {
        UserProfile profile = getCurrentUserProfile();
        return profile.getEmergencyContacts() != null ? profile.getEmergencyContacts() : new ArrayList<>();
    }

    @Override
    public UserProfileResponse addDevice(UserDeviceRequest request) {
        UserProfile profile = getCurrentUserProfile();
        if (profile.getUserDevices() == null) {
            profile.setUserDevices(new ArrayList<>());
        }

        java.util.Optional<UserDevice> existing = profile.getUserDevices().stream()
                .filter(d -> d.getDeviceToken().equals(request.getDeviceToken()))
                .findFirst();

        if (existing.isPresent()) {
            UserDevice d = existing.get();
            d.setDeviceName(request.getDeviceName());
            d.setDeviceType(request.getDeviceType());
            d.setActive(request.isActive());
            d.setLastSeenAt(java.time.Instant.now());
        } else {
            UserDevice device = UserDevice.builder()
                    .deviceName(request.getDeviceName())
                    .deviceToken(request.getDeviceToken())
                    .deviceType(request.getDeviceType())
                    .isActive(request.isActive())
                    .lastSeenAt(java.time.Instant.now())
                    .build();
            profile.getUserDevices().add(device);
        }
        return userProfileMapper.toResponse(userProfileRepository.save(profile));
    }

    @Override
    public UserProfileResponse updateDevice(String deviceId, UserDeviceRequest request) {
        UserProfile profile = getCurrentUserProfile();
        List<UserDevice> devices = profile.getUserDevices();
        if (devices == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Device list is empty");
        }
        UserDevice deviceToUpdate = devices.stream()
                .filter(d -> d.getId().equals(deviceId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found"));

        deviceToUpdate.setDeviceName(request.getDeviceName());
        deviceToUpdate.setDeviceToken(request.getDeviceToken());
        deviceToUpdate.setDeviceType(request.getDeviceType());
        deviceToUpdate.setActive(request.isActive());
        deviceToUpdate.setLastSeenAt(java.time.Instant.now());

        return userProfileMapper.toResponse(userProfileRepository.save(profile));
    }

    @Override
    public UserProfileResponse deleteDevice(String deviceId) {
        UserProfile profile = getCurrentUserProfile();
        List<UserDevice> devices = profile.getUserDevices();
        if (devices == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Device list is empty");
        }
        boolean removed = devices.removeIf(d -> d.getId().equals(deviceId));
        if (!removed) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found");
        }
        return userProfileMapper.toResponse(userProfileRepository.save(profile));
    }

    @Override
    public List<UserDevice> getMyDevices() {
        UserProfile profile = getCurrentUserProfile();
        return profile.getUserDevices() != null ? profile.getUserDevices() : new ArrayList<>();
    }

    @Override
    public Page<UserProfileSummaryResponse> searchProfiles(String keyword, Role role, Pageable pageable) {
        String currentProfileId = getProfileIdFromRequest();
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