package ct01.n07.backend.service;

public interface SecurityContextService {
    /**
     * Retrieves the userId of the currently authenticated user from SecurityContext.
     * @return the userId as a string.
     */
    String getCurrentUserId();

    /**
     * Retrieves the profileId from the current HTTP Request context.
     * @return the profileId or null if not present.
     */
    String getCurrentProfileId();

    /**
     * Validates if the user is authenticated and optionally retrieves the user ID.
     */
    void requireAuthenticated();
}
