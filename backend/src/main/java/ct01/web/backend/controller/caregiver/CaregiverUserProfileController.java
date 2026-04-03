package ct01.web.backend.controller.caregiver;

import ct01.web.backend.dto.userProfile.UserProfileRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/caregiver/user-profile")
public class CaregiverUserProfileController {
    @PostMapping("/create-profile")
    public void createElderlyProfile(@RequestBody UserProfileRequest request) {

    }
}
