package ct01.n07.backend.service;

import ct01.n07.backend.dto.auth.*;
import ct01.n07.backend.model.User;
import ct01.n07.backend.model.enums.UserStatus;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    User findById(String id);

    User findByEmail(String email);

    User createUser(LoginRequest loginRequest);

    User createUser(LoginRequest loginRequest, UserStatus status);

    User verifyUserCredentials(@NotBlank String email, @NotBlank String password);

    Page<AdminUserResponse> getAllUsers(Pageable pageable);

    AdminUserResponse adminCreateUser(AdminUserCreateRequest request);

    AdminUserResponse adminUpdateUser(String id, AdminUserUpdateRequest request);

    void deleteUser(String id);

    AdminUserResponse lockUser(String id);

    AdminUserResponse unlockUser(String id);

    void updatePassword(String email, String newPassword);

    User updateStatus(String id, UserStatus status);
}
