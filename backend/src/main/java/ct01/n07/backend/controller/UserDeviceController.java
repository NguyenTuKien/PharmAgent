package ct01.n07.backend.controller;

import ct01.n07.backend.dto.user.UserDeviceRequest;
import ct01.n07.backend.dto.user.UserDeviceResponse;
import ct01.n07.backend.service.UserDeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/devices")
public class UserDeviceController {

    private final UserDeviceService userDeviceService;

    @GetMapping("/me")
    public ResponseEntity<List<UserDeviceResponse>> getMyDevices() {
        return ResponseEntity.ok(userDeviceService.getMyDevices());
    }

    @PostMapping("/me")
    public ResponseEntity<UserDeviceResponse> addDevice(
            @Valid @RequestBody UserDeviceRequest request) {
        return ResponseEntity.ok(userDeviceService.addDevice(request));
    }

    @PutMapping("/me/{deviceId}")
    public ResponseEntity<UserDeviceResponse> updateDevice(
            @PathVariable String deviceId,
            @Valid @RequestBody UserDeviceRequest request) {
        return ResponseEntity.ok(userDeviceService.updateDevice(deviceId, request));
    }

    @DeleteMapping("/me/{deviceId}")
    public ResponseEntity<Void> deleteDevice(
            @PathVariable String deviceId) {
        userDeviceService.deleteDevice(deviceId);
        return ResponseEntity.noContent().build();
    }
}
