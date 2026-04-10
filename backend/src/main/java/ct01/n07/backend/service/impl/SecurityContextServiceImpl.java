package ct01.n07.backend.service.impl;

import ct01.n07.backend.constant.ProfileConstant;
import ct01.n07.backend.service.SecurityContextService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SecurityContextServiceImpl implements SecurityContextService {

    private static final String PROFILE_ID_ATTR = "profileId";

    @Override
    public String getCurrentUserId() {
        requireAuthenticated();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof String userId) || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, ProfileConstant.INVALID_AUTHENTICATION_PRINCIPAL);
        }
        return userId;
    }

    @Override
    public String getCurrentProfileId() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;
        return request != null ? (String) request.getAttribute(PROFILE_ID_ATTR) : null;
    }

    @Override
    public void requireAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, ProfileConstant.UNAUTHORIZED);
        }
    }
}
