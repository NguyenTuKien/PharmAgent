package ct01.web.backend.service.impl;

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

    @Override
    public void saveUserProfile(UserProfile userProfile) {
        userProfileRepository.save(userProfile);
    }

    @Override
    public UserProfile findById(String profileId) {
        return userProfileRepository.findById(profileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
    }

    @Override
    public Page<UserProfile> findAllByUserId(String userId, Pageable pageable) {
        return userProfileRepository.findAllByUserId(userId, pageable);
    }

    @Override
    public UserProfile getCurrentUserProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof String userId) || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authentication principal");
        }

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;
        String profileId = request != null ? (String) request.getAttribute(PROFILE_ID_ATTR) : null;

        // Creator-sensitive actions must run under profile token, not access token.
        if (profileId == null || profileId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Profile token required");
        }

        UserProfile profile = findById(profileId);
        if (!userId.equals(profile.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Profile does not belong to current user");
        }
        return profile;
    }
}