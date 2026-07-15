package com.placesync.auth.repository;

import com.placesync.auth.entity.RefreshToken;
import com.placesync.common.SharedPostgresContainer;
import com.placesync.user.entity.User;
import com.placesync.user.entity.UserRole;
import com.placesync.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
class RefreshTokenRepositoryTest {

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

    @Autowired RefreshTokenRepository refreshTokenRepository;
    @Autowired UserRepository userRepository;

    User user;
    UUID familyId;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
                .email("user@test.com").passwordHash("hash").role(UserRole.ROLE_STUDENT).build());
        familyId = UUID.randomUUID();
    }

    private RefreshToken saveToken(String hash, boolean revoked) {
        return refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .tokenHash(hash)
                .familyId(familyId)
                .expiresAt(OffsetDateTime.now().plusDays(30))
                .isRevoked(revoked)
                .revokedAt(revoked ? OffsetDateTime.now() : null)
                .build());
    }

    @Test
    void findByTokenHashAndIsRevokedFalse_activeToken_returnsToken() {
        saveToken("hash-active", false);

        Optional<RefreshToken> result = refreshTokenRepository.findByTokenHashAndIsRevokedFalse("hash-active");

        assertThat(result).isPresent();
    }

    @Test
    void findByTokenHashAndIsRevokedFalse_revokedToken_returnsEmpty() {
        saveToken("hash-revoked", true);

        Optional<RefreshToken> result = refreshTokenRepository.findByTokenHashAndIsRevokedFalse("hash-revoked");

        assertThat(result).isEmpty();
    }

    @Test
    void findByTokenHash_revokedToken_stillReturnsToken() {
        saveToken("hash-any", true);

        Optional<RefreshToken> result = refreshTokenRepository.findByTokenHash("hash-any");

        assertThat(result).isPresent();
        assertThat(result.get().getIsRevoked()).isTrue();
    }

    @Test
    void deleteByFamilyId_removesAllFamilyTokens() {
        saveToken("hash-1", false);
        saveToken("hash-2", false);

        refreshTokenRepository.deleteByFamilyId(familyId);

        assertThat(refreshTokenRepository.findByTokenHash("hash-1")).isEmpty();
        assertThat(refreshTokenRepository.findByTokenHash("hash-2")).isEmpty();
    }

    @Test
    void deleteByUserId_removesAllUserTokens() {
        saveToken("hash-user-1", false);
        saveToken("hash-user-2", true);

        refreshTokenRepository.deleteByUserId(user.getId());

        assertThat(refreshTokenRepository.findAll()).isEmpty();
    }
}
