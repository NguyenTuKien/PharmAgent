package ct01.n07.backend.controller;

import ct01.n07.backend.dto.auth.*;
import ct01.n07.backend.facade.AuthFacade;
import ct01.n07.backend.facade.PasswordFacade;
import ct01.n07.backend.facade.RegistrationFacade;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
    private final AuthFacade authFacade;
    private final RegistrationFacade registrationFacade;
    private final PasswordFacade passwordFacade;

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
        String accessToken = authFacade.selectProfile(authorization, profileId);
        return ResponseEntity.ok(Map.of("accessToken", accessToken));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponse> refresh(@Valid @RequestBody TokenRefreshRequest request) {
        return ResponseEntity.ok(authFacade.refresh(request));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthMessageResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(registrationFacade.register(registerRequest));
    }


    @PostMapping("/verify-email")
    public ResponseEntity<AuthMessageResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return ResponseEntity.ok(registrationFacade.verifyEmail(request));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<AuthMessageResponse> resendVerification(
            @Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(registrationFacade.resendVerificationEmail(request.getEmail()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody LogoutRequest request) {
        authFacade.logout(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<AuthMessageResponse> forgotPassword(
            @RequestParam(required = false) String email,
            @Valid @RequestBody(required = false) ForgotPasswordRequest request) {
        String targetEmail = request != null ? request.getEmail() : email;
        if (targetEmail == null || targetEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email không được để trống");
        }

        passwordFacade.processForgotPassword(targetEmail);
        return ResponseEntity.ok(AuthMessageResponse.builder()
                .email(targetEmail.trim().toLowerCase())
                .message("Nếu email tồn tại, mã OTP đặt lại mật khẩu đã được gửi.")
                .build());
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordFacade.resetPassword(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            java.security.Principal principal) {
        passwordFacade.changePassword(principal.getName(), request);
        return ResponseEntity.ok().build();
    }
}
