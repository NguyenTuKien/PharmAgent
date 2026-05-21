package ct01.n07.backend.controller.caregiver;

import ct01.n07.backend.dto.user.UserProfileSummaryResponse;
import ct01.n07.backend.dto.user.CreateProfileRequest;
import ct01.n07.backend.dto.user.UpdateProfileRequest;
import ct01.n07.backend.dto.user.UserProfileResponse;
import ct01.n07.backend.model.enums.Role;
import ct01.n07.backend.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;



/**
 * Các thao tác quản lý profile chỉ dành cho CAREGIVER.
 * Phân quyền được cấu hình tập trung tại SecurityConfiguration.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/caregiver/profiles")
public class CaregiverProfileController {

    private final UserProfileService userProfileService;

    // ── Chỉ CAREGIVER mới được tạo profile mới trong tài khoản ──
    @PostMapping
    public ResponseEntity<UserProfileResponse> createProfile(
            @Valid @RequestBody CreateProfileRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userProfileService.createManagedElderlyProfile(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserProfileResponse> getProfile(@PathVariable String id) {
        return ResponseEntity.ok(userProfileService.getManagedElderlyProfile(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @PathVariable String id,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userProfileService.updateManagedElderlyProfile(id, request));
    }

    // ── Chỉ CAREGIVER mới được xóa profile trong tài khoản ──
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProfile(@PathVariable String id) {
        userProfileService.deleteManagedElderlyProfile(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/search")
    public ResponseEntity<Page<UserProfileSummaryResponse>> searchElderlyProfiles(
            @RequestParam(required = false, defaultValue = "") String query,
            @RequestParam(required = false) Role role,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<UserProfileSummaryResponse> results = userProfileService.searchProfiles(query, role, pageable);
        return ResponseEntity.ok(results);
    }
}
