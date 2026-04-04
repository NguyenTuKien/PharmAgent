package ct01.web.backend.constant;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public final class ProfileConstant {
    public static final String UNAUTHORIZED = "Unauthorized";
    public static final String INVALID_AUTHENTICATION_PRINCIPAL = "Invalid authentication principal";
    public static final String ACCESS_TOKEN_REQUIRED = "Access token is required for this API";
    public static final String PROFILE_TOKEN_REQUIRED = "Profile token is required for this API";

    public static final String PROFILE_NOT_FOUND = "Profile not found";
    public static final String PROFILE_NOT_BELONG_TO_USER = "Profile does not belong to current user";
    public static final String PROFILE_ALREADY_DELETED = "Profile is already deleted";
    public static final String PHONE_ALREADY_EXISTS = "Phone already exists";
}