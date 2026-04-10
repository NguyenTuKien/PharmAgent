package ct01.n07.backend.security;

import ct01.n07.backend.constant.ProfileConstant;
import ct01.n07.backend.model.UserProfile;
import ct01.n07.backend.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class ProfileAccessContext {

    private static final String PROFILE_ID_ATTR = "profileId";

    private final UserProfileRepository userProfileRepository;

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

    public String getCurrentProfileId() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        return (String) attributes.getRequest().getAttribute(PROFILE_ID_ATTR);
    }

    public UserProfile getCurrentUserProfile() {
        String userId = getCurrentUserId();
        String profileId = getCurrentProfileId();

        if (profileId == null || profileId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, ProfileConstant.PROFILE_TOKEN_REQUIRED);
        }

        UserProfile profile = userProfileRepository.findById(profileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ProfileConstant.PROFILE_NOT_FOUND));

        if (!userId.equals(profile.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ProfileConstant.PROFILE_NOT_BELONG_TO_USER);
        }
        return profile;
    }
}


