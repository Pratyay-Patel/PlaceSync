package com.placesync.notification.repository;

import com.placesync.notification.entity.Notification;
import com.placesync.notification.entity.NotificationType;
import com.placesync.user.entity.User;
import com.placesync.user.entity.UserRole;
import com.placesync.user.repository.UserRepository;
import com.placesync.common.SharedPostgresContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
class NotificationRepositoryTest {

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", SharedPostgresContainer.POSTGRES::getJdbcUrl);
        r.add("spring.datasource.username", SharedPostgresContainer.POSTGRES::getUsername);
        r.add("spring.datasource.password", SharedPostgresContainer.POSTGRES::getPassword);
        r.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        r.add("spring.flyway.enabled", () -> "true");
        r.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        r.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Autowired NotificationRepository notificationRepository;
    @Autowired UserRepository userRepository;

    User user;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
                .email("user@test.com").passwordHash("hash").role(UserRole.ROLE_STUDENT).build());
    }

    private Notification save(boolean isRead) {
        return notificationRepository.save(Notification.builder()
                .user(user)
                .type(NotificationType.APPLICATION_SUBMITTED)
                .title("Test notification")
                .body("Body text")
                .isRead(isRead)
                .readAt(isRead ? OffsetDateTime.now() : null)
                .build());
    }

    @Test
    void findByUserIdOrderByCreatedAtDesc_returnsAll() {
        save(false);
        save(true);

        Page<Notification> result = notificationRepository.findByUserIdOrderByCreatedAtDesc(
                user.getId(), PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void findByUserIdAndIsReadFalse_returnsOnlyUnread() {
        save(false);
        save(true);

        Page<Notification> unread = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(
                user.getId(), PageRequest.of(0, 10));

        assertThat(unread.getTotalElements()).isEqualTo(1);
    }

    @Test
    void countByUserIdAndIsReadFalse_correctCount() {
        save(false);
        save(false);
        save(true);

        long count = notificationRepository.countByUserIdAndIsReadFalse(user.getId());

        assertThat(count).isEqualTo(2);
    }

    @Test
    void markAllAsRead_updatesUnreadNotifications() {
        save(false);
        save(false);

        int updated = notificationRepository.markAllAsRead(user.getId(), OffsetDateTime.now());

        assertThat(updated).isEqualTo(2);
        assertThat(notificationRepository.countByUserIdAndIsReadFalse(user.getId())).isZero();
    }
}
