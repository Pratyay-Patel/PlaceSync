package com.placesync.user.repository;

import com.placesync.user.entity.User;
import com.placesync.user.entity.UserRole;
import com.placesync.common.SharedPostgresContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
class UserRepositoryTest {

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

    @Autowired UserRepository userRepository;

    private User buildUser(String email) {
        return User.builder().email(email).passwordHash("hash").role(UserRole.ROLE_STUDENT).build();
    }

    @Test
    void findByEmailAndDeletedAtIsNull_activeUser_returnsUser() {
        User saved = userRepository.save(buildUser("active@test.com"));

        Optional<User> result = userRepository.findByEmailAndDeletedAtIsNull("active@test.com");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(saved.getId());
    }

    @Test
    void findByEmailAndDeletedAtIsNull_softDeletedUser_returnsEmpty() {
        User user = buildUser("deleted@test.com");
        user.setDeletedAt(OffsetDateTime.now());
        userRepository.save(user);

        Optional<User> result = userRepository.findByEmailAndDeletedAtIsNull("deleted@test.com");

        assertThat(result).isEmpty();
    }

    @Test
    void existsByEmail_existingEmail_returnsTrue() {
        userRepository.save(buildUser("exists@test.com"));

        assertThat(userRepository.existsByEmail("exists@test.com")).isTrue();
    }

    @Test
    void existsByEmail_missingEmail_returnsFalse() {
        assertThat(userRepository.existsByEmail("nobody@test.com")).isFalse();
    }

    @Test
    void countByRole_correctCount() {
        userRepository.save(buildUser("s1@test.com"));
        userRepository.save(buildUser("s2@test.com"));

        long count = userRepository.countByRole(UserRole.ROLE_STUDENT);

        assertThat(count).isGreaterThanOrEqualTo(2);
    }
}
