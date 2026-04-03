package ct01.web.backend.controller;

import ct01.web.backend.dto.auth.UserProfileSummaryResponse;
import ct01.web.backend.mapper.UserProfileMapper;
import ct01.web.backend.model.UserProfile;
import ct01.web.backend.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user-profiles")
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final UserProfileMapper userProfileMapper;

    @GetMapping("")
    public ResponseEntity<Page<UserProfileSummaryResponse>> findAllByUserId(
            @PageableDefault(page = 0, size = 10) Pageable pageable) {
        String userId = userProfileService.getCurrentUserProfile().getUserId();
        Page<UserProfile> userProfilePage = userProfileService.findAllByUserId(userId, pageable);
        return ResponseEntity.ok(
                userProfilePage.map(profile -> userProfileMapper.toSummary(profile))
        );
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileSummaryResponse> getCurrentUserProfile() {
        UserProfile userProfile = userProfileService.getCurrentUserProfile();
        return ResponseEntity.ok(userProfileMapper.toSummary(userProfile));
    }
}