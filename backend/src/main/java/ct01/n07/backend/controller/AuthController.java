package ct01.n07.backend.controller;

import ct01.n07.backend.dto.auth.*;
import ct01.n07.backend.facade.AuthFacade;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
    private final AuthFacade authFacade;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            @PageableDefault(page = 0, size = 10) Pageable pageable) {
        return ResponseEntity.ok(authFacade.login(request, pageable));
    }

    @PostMapping("/profiles/{profileId}/select")
    public ResponseEntity<Map<String, String>> selectProfile(
            @RequestHeader("Authorization") String authorization,
            @PathVariable("profileId") String profileId) { // Dùng PathVariable
        String profileToken = authFacade.selectProfile(authorization, profileId);
        return ResponseEntity.ok(Map.of("profileToken", profileToken));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponse> refresh(@Valid @RequestBody TokenRefreshRequest request) {
        return ResponseEntity.ok(authFacade.refresh(request));
    }

    @PostMapping("/signup")
    public ResponseEntity<LoginResponse> signup(
            @PageableDefault(page = 0, size = 10) Pageable pageable,
            @Valid @RequestBody SignupRequest signupRequest) {
        return ResponseEntity.ok(authFacade.signup(signupRequest, pageable));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody LogoutRequest request) {
        authFacade.logout(request);
        return ResponseEntity.noContent().build();
    }
}
