package com.placesync.common.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID>, JpaSpecificationExecutor<AuditLog> {

    Page<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, UUID entityId, Pageable pageable);

    Page<AuditLog> findByActorIdOrderByCreatedAtDesc(UUID actorId, Pageable pageable);
}
