package com.placesync.notification.service;

import com.placesync.common.exception.ResourceNotFoundException;
import com.placesync.common.util.PagedResponse;
import com.placesync.notification.dto.NotificationResponse;
import com.placesync.notification.entity.Notification;
import com.placesync.notification.entity.NotificationType;
import com.placesync.notification.mapper.NotificationMapper;
import com.placesync.notification.repository.NotificationRepository;
import com.placesync.user.entity.User;
import com.placesync.user.repository.UserRepository;
import static com.placesync.common.util.LogSanitizer.sanitize;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@SuppressWarnings("java:S2629")
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationMapper notificationMapper;

    @Transactional
    public void createForUser(UUID userId, NotificationType type, String title, String body,
                               UUID referenceId, String referenceType) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .body(body)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .build();
        notificationRepository.save(notification);
        log.info("Created notification type={} for userId={}", type, userId);
    }

    @Transactional(readOnly = true)
    public PagedResponse<NotificationResponse> getNotifications(UUID userId, boolean unreadOnly, Pageable pageable) {
        return PagedResponse.of(
                unreadOnly
                        ? notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId, pageable).map(notificationMapper::toResponse)
                        : notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable).map(notificationMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public long countUnread(UUID userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public NotificationResponse markAsRead(UUID userId, UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));
        if (!notification.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Notification does not belong to the requesting user");
        }
        if (!notification.getIsRead()) {
            notification.setIsRead(true);
            notification.setReadAt(OffsetDateTime.now());
            notificationRepository.save(notification);
        }
        return notificationMapper.toResponse(notification);
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        int updated = notificationRepository.markAllAsRead(userId, OffsetDateTime.now());
        log.info("Marked {} notifications as read for userId={}", updated, sanitize(userId));
    }
}
