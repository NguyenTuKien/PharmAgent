package ct01.web.backend.controller;

import ct01.web.backend.dto.userProfile.CreateProfileRequest;
import ct01.web.backend.dto.userProfile.UpdateProfileRequest;
import ct01.web.backend.dto.userProfile.UserProfileResponse;
import ct01.web.backend.dto.userProfile.UserProfileSummaryResponse;
import ct01.web.backend.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/profiles")
public class UserProfileController {
    // Cần loại bỏ user hiện tại khỏi list active profiles trước khi xóa
    private final UserProfileService userProfileService;

    @GetMapping("")
    public ResponseEntity<Page<UserProfileSummaryResponse>> getProfiles(
            @PageableDefault(page = 0, size = 10) Pageable pageable) {
        return ResponseEntity.ok(userProfileService.getProfiles(pageable));
    }

    @PostMapping("")
    public ResponseEntity<UserProfileResponse> createProfile(@Valid @RequestBody CreateProfileRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userProfileService.createProfile(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProfile(@PathVariable String id) {
        userProfileService.deleteProfile(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUserProfile() {
        return ResponseEntity.ok(userProfileService.getMyProfile());
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateCurrentUserProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userProfileService.updateMyProfile(request));
    }
}