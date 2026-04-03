package ct01.web.backend.service;

import ct01.web.backend.dto.auth.LoginRequest;
import ct01.web.backend.model.User;
import jakarta.validation.constraints.NotBlank;

public interface UserService {
    User findById(String id);

    User findByEmail(String email);

    User createUser(LoginRequest loginRequest);

    User verifyUserCredentials(@NotBlank String email, @NotBlank String password);
}
