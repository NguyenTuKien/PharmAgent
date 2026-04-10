package ct01.n07.backend.service;

import ct01.n07.backend.dto.notification.NotificationCreateRequest;
import ct01.n07.backend.dto.notification.NotificationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import ct01.n07.backend.model.Notification;

public interface NotificationService {
    NotificationResponse sendNotification(NotificationCreateRequest request);
    Page<NotificationResponse> getMyNotifications(Pageable pageable);
    void saveAllNotifications(List<Notification> notifications);
}
