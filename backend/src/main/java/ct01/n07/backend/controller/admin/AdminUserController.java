package ct01.n07.backend.controller.admin;

import ct01.n07.backend.dto.auth.AdminUserCreateRequest;
import ct01.n07.backend.dto.auth.AdminUserResponse;
import ct01.n07.backend.dto.auth.AdminUserUpdateRequest;
import ct01.n07.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<Page<AdminUserResponse>> getAllUsers(
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(userService.getAllUsers(pageable));
    }

    @PostMapping
    public ResponseEntity<AdminUserResponse> createUser(@Valid @RequestBody AdminUserCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.adminCreateUser(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminUserResponse> updateUser(
            @PathVariable String id,
            @Valid @RequestBody AdminUserUpdateRequest request
    ) {
        return ResponseEntity.ok(userService.adminUpdateUser(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/lock")
    public ResponseEntity<AdminUserResponse> lockUser(@PathVariable String id) {
        return ResponseEntity.ok(userService.lockUser(id));
    }

    @PatchMapping("/{id}/unlock")
    public ResponseEntity<AdminUserResponse> unlockUser(@PathVariable String id) {
        return ResponseEntity.ok(userService.unlockUser(id));
    }
}
