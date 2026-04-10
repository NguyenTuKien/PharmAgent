package ct01.n07.backend.service.impl;

import ct01.n07.backend.dto.notification.NotificationCreateRequest;
import ct01.n07.backend.dto.notification.NotificationResponse;
import ct01.n07.backend.mapper.NotificationMapper;
import ct01.n07.backend.model.Notification;
import ct01.n07.backend.model.UserProfile;
import ct01.n07.backend.model.enums.NotificationStatus;
import ct01.n07.backend.repository.NotificationRepository;
import ct01.n07.backend.repository.UserProfileRepository;
import ct01.n07.backend.security.ProfileAccessContext;
import ct01.n07.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserProfileRepository userProfileRepository;
    private final ProfileAccessContext profileAccessContext;
    private final NotificationMapper notificationMapper;

    @Override
    public NotificationResponse sendNotification(NotificationCreateRequest request) {
        UserProfile sender = profileAccessContext.getCurrentUserProfile();
        
        // Validate receiver exists
        if (!userProfileRepository.existsById(request.getReceiverId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Người nhận không tồn tại");
        }

        Notification notification = notificationMapper.toEntity(request, sender.getId());
        notification.setStatus(NotificationStatus.SUCCESS);
        
        Notification saved = notificationRepository.save(notification);
        log.info("Notification sent from {} to {}", sender.getId(), request.getReceiverId());
        
        return notificationMapper.toResponse(saved);
    }

    @Override
    public Page<NotificationResponse> getMyNotifications(Pageable pageable) {
        UserProfile currentUser = profileAccessContext.getCurrentUserProfile();
        
        Page<Notification> notifications = notificationRepository.findByReceiverIdAndStatus(
                currentUser.getId(), NotificationStatus.SUCCESS, pageable);
        
        return notifications.map(notificationMapper::toResponse);
    }

    @Override
    public void saveAllNotifications(java.util.List<Notification> notifications) {
        notificationRepository.saveAll(notifications);
    }
}
