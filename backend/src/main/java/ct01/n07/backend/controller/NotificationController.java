package ct01.n07.backend.controller;

import ct01.n07.backend.dto.notification.NotificationCreateRequest;
import ct01.n07.backend.dto.notification.NotificationResponse;
import ct01.n07.backend.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    public ResponseEntity<NotificationResponse> sendNotification(@Valid @RequestBody NotificationCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(notificationService.sendNotification(request));
    }

    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getMyNotifications(
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(notificationService.getMyNotifications(pageable));
    }

    @GetMapping("/sent")
    public ResponseEntity<Page<NotificationResponse>> getSentNotifications(
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(notificationService.getSentNotifications(pageable));
    }
}
