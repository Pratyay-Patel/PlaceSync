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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock NotificationRepository notificationRepository;
    @Mock UserRepository userRepository;
    @Mock NotificationMapper notificationMapper;

    @InjectMocks NotificationService notificationService;

    private final UUID userId = UUID.randomUUID();
    private final UUID notificationId = UUID.randomUUID();

    @Test
    void createForUser_validUser_savesNotification() {
        User user = User.builder().build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        notificationService.createForUser(userId, NotificationType.APPLICATION_SUBMITTED,
                "Title", "Body", UUID.randomUUID(), "Application");

        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void createForUser_userNotFound_throwsResourceNotFoundException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        UUID refId = UUID.randomUUID();
        assertThatThrownBy(() -> notificationService.createForUser(userId,
                NotificationType.APPLICATION_SUBMITTED, "T", "B", refId, "Application"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getNotifications_allNotifications_returnsMappedPage() {
        Notification notification = Notification.builder().build();
        Pageable pageable = PageRequest.of(0, 20);
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable))
                .thenReturn(new PageImpl<>(List.of(notification)));
        when(notificationMapper.toResponse(notification)).thenReturn(NotificationResponse.builder().build());

        PagedResponse<NotificationResponse> result = notificationService.getNotifications(userId, false, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void getNotifications_unreadOnly_usesUnreadQuery() {
        Pageable pageable = PageRequest.of(0, 20);
        when(notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId, pageable))
                .thenReturn(new PageImpl<>(List.of()));

        notificationService.getNotifications(userId, true, pageable);

        verify(notificationRepository).findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId, pageable);
        verify(notificationRepository, never()).findByUserIdOrderByCreatedAtDesc(any(), any());
    }

    @Test
    void countUnread_delegatesToRepository() {
        when(notificationRepository.countByUserIdAndIsReadFalse(userId)).thenReturn(3L);

        long count = notificationService.countUnread(userId);

        assertThat(count).isEqualTo(3L);
    }

    @Test
    void markAsRead_ownedNotification_setsReadFlag() {
        User user = User.builder().build();
        user.setId(userId);
        Notification notification = Notification.builder().user(user).isRead(false).build();
        NotificationResponse response = NotificationResponse.builder().isRead(true).build();
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
        when(notificationMapper.toResponse(notification)).thenReturn(response);

        NotificationResponse result = notificationService.markAsRead(userId, notificationId);

        assertThat(result.getIsRead()).isTrue();
        verify(notificationRepository).save(notification);
    }

    @Test
    void markAsRead_alreadyRead_doesNotSaveAgain() {
        User user = User.builder().build();
        user.setId(userId);
        Notification notification = Notification.builder().user(user).isRead(true).build();
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
        when(notificationMapper.toResponse(notification)).thenReturn(NotificationResponse.builder().build());

        notificationService.markAsRead(userId, notificationId);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markAsRead_notificationBelongsToOtherUser_throwsAccessDeniedException() {
        User owner = User.builder().build();
        owner.setId(UUID.randomUUID());
        Notification notification = Notification.builder().user(owner).build();
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.markAsRead(userId, notificationId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void markAsRead_notFound_throwsResourceNotFoundException() {
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(userId, notificationId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void markAllAsRead_callsBulkUpdate() {
        when(notificationRepository.markAllAsRead(eq(userId), any(OffsetDateTime.class))).thenReturn(5);

        notificationService.markAllAsRead(userId);

        verify(notificationRepository).markAllAsRead(eq(userId), any(OffsetDateTime.class));
    }
}
