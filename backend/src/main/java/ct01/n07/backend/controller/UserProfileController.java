package ct01.n07.backend.controller;

import ct01.n07.backend.dto.user.UserContactRequest;
import ct01.n07.backend.dto.user.UpdateProfileRequest;
import ct01.n07.backend.dto.user.UserDeviceRequest;
import ct01.n07.backend.dto.user.UserProfileResponse;
import ct01.n07.backend.dto.user.UserProfileSummaryResponse;
import ct01.n07.backend.model.UserContact;
import ct01.n07.backend.model.UserDevice;
import ct01.n07.backend.service.UserContactService;
import ct01.n07.backend.service.UserDeviceService;
import ct01.n07.backend.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/profiles")
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final UserContactService userContactService;
    private final UserDeviceService userDeviceService;

    // ── Mọi role đều có thể xem danh sách profile trong tài khoản ──
    @GetMapping
    public ResponseEntity<Page<UserProfileSummaryResponse>> getProfiles(
            @PageableDefault(page = 0, size = 10) Pageable pageable) {
        return ResponseEntity.ok(userProfileService.getProfiles(pageable));
    }

    // ── Mọi role đều có thể xem và sửa profile của chính mình ──
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile() {
        return ResponseEntity.ok(userProfileService.getMyProfile());
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateMyProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userProfileService.updateMyProfile(request));
    }

    // ── Quản lý liên hệ người dùng (User Contacts) ──

    @GetMapping("/me/contacts")
    public ResponseEntity<List<UserContact>> getMyContacts() {
        return ResponseEntity.ok(userContactService.getMyContacts());
    }

    @PostMapping("/me/contacts")
    public ResponseEntity<UserProfileResponse> addUserContact(
            @Valid @RequestBody UserContactRequest request) {
        return ResponseEntity.ok(userContactService.addUserContact(request));
    }

    @PutMapping("/me/contacts/{contactId}")
    public ResponseEntity<UserProfileResponse> updateUserContact(
            @PathVariable String contactId,
            @Valid @RequestBody UserContactRequest request) {
        return ResponseEntity.ok(userContactService.updateUserContact(contactId, request));
    }

    @DeleteMapping("/me/contacts/{contactId}")
    public ResponseEntity<UserProfileResponse> deleteContact(
            @PathVariable String contactId) {
        return ResponseEntity.ok(userContactService.deleteContact(contactId));
    }

    // ── Quản lý thiết bị (User Devices) ──

    @GetMapping("/me/devices")
    public ResponseEntity<List<UserDevice>> getMyDevices() {
        return ResponseEntity.ok(userDeviceService.getMyDevices());
    }

    @PostMapping("/me/devices")
    public ResponseEntity<UserProfileResponse> addDevice(
            @Valid @RequestBody UserDeviceRequest request) {
        return ResponseEntity.ok(userDeviceService.addDevice(request));
    }

    @PutMapping("/me/devices/{deviceId}")
    public ResponseEntity<UserProfileResponse> updateDevice(
            @PathVariable String deviceId,
            @Valid @RequestBody UserDeviceRequest request) {
        return ResponseEntity.ok(userDeviceService.updateDevice(deviceId, request));
    }

    @DeleteMapping("/me/devices/{deviceId}")
    public ResponseEntity<UserProfileResponse> deleteDevice(
            @PathVariable String deviceId) {
        return ResponseEntity.ok(userDeviceService.deleteDevice(deviceId));
    }
}