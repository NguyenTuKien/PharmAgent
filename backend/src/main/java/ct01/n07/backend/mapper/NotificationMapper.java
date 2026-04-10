package ct01.n07.backend.mapper;

import ct01.n07.backend.dto.notification.NotificationCreateRequest;
import ct01.n07.backend.dto.notification.NotificationResponse;
import ct01.n07.backend.model.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public Notification toEntity(NotificationCreateRequest request, String senderId) {
        if (request == null) {
            return null;
        }
        return Notification.builder()
                .senderId(senderId)
                .receiverId(request.getReceiverId())
                .content(request.getContent())
                .build();
    }

    public NotificationResponse toResponse(Notification notification) {
        if (notification == null) {
            return null;
        }
        return NotificationResponse.builder()
                .id(notification.getId())
                .senderId(notification.getSenderId())
                .receiverId(notification.getReceiverId())
                .content(notification.getContent())
                .status(notification.getStatus())
                .sentAt(notification.getSentAt())
                .build();
    }
}
