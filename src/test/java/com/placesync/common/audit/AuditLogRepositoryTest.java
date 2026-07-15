package com.placesync.common.audit;

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

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
class AuditLogRepositoryTest {

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", com.placesync.common.SharedPostgresContainer.POSTGRES::getJdbcUrl);
        r.add("spring.datasource.username", com.placesync.common.SharedPostgresContainer.POSTGRES::getUsername);
        r.add("spring.datasource.password", com.placesync.common.SharedPostgresContainer.POSTGRES::getPassword);
        r.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        r.add("spring.flyway.enabled", () -> "true");
        r.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        r.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Autowired AuditLogRepository auditLogRepository;

    private AuditLog saveLog(String entityType, UUID entityId, UUID actorId) {
        return auditLogRepository.save(AuditLog.builder()
                .entityType(entityType)
                .entityId(entityId)
                .action(AuditAction.CREATE)
                .actorId(actorId)
                .actorRole("ROLE_ADMIN")
                .actorEmail("admin@test.com")
                .ipAddress("127.0.0.1")
                .newValues(Map.of("key", "value"))
                .build());
    }

    @Test
    void findByEntityTypeAndEntityId_returnsMatchingLogs() {
        UUID entityId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        saveLog("Job", entityId, actorId);
        saveLog("Job", UUID.randomUUID(), actorId);

        Page<AuditLog> result = auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
                "Job", entityId, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getEntityId()).isEqualTo(entityId);
    }

    @Test
    void findByActorId_returnsAllActorLogs() {
        UUID actorId = UUID.randomUUID();
        saveLog("Job", UUID.randomUUID(), actorId);
        saveLog("User", UUID.randomUUID(), actorId);
        saveLog("Job", UUID.randomUUID(), UUID.randomUUID());

        Page<AuditLog> result = auditLogRepository.findByActorIdOrderByCreatedAtDesc(
                actorId, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void saveLog_withJsonbNewValues_persistsCorrectly() {
        UUID entityId = UUID.randomUUID();
        AuditLog saved = saveLog("Application", entityId, UUID.randomUUID());

        AuditLog found = auditLogRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getNewValues()).containsEntry("key", "value");
    }

    @Test
    void saveLog_withInetIpAddress_persistsCorrectly() {
        AuditLog saved = saveLog("User", UUID.randomUUID(), UUID.randomUUID());

        AuditLog found = auditLogRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getIpAddress()).isEqualTo("127.0.0.1");
    }
}
