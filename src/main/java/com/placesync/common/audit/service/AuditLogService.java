package com.placesync.common.audit.service;

import com.placesync.common.audit.AuditAction;
import com.placesync.common.audit.AuditLog;
import com.placesync.common.audit.AuditLogRepository;
import com.placesync.common.audit.dto.AuditLogResponse;
import com.placesync.common.audit.dto.AuditSearchRequest;
import com.placesync.common.audit.mapper.AuditMapper;
import com.placesync.common.exception.ResourceNotFoundException;
import com.placesync.common.util.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository auditLogRepository;
    private final AuditMapper auditMapper;

    @Async
    public void saveAsync(AuditLog auditLog) {
        try {
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.warn("Failed to persist audit log entry: action={}, entityType={}, entityId={}",
                    auditLog.getAction(), auditLog.getEntityType(), auditLog.getEntityId(), e);
        }
    }

    @Transactional(readOnly = true)
    public PagedResponse<AuditLogResponse> search(AuditSearchRequest req, Pageable pageable) {
        Specification<AuditLog> spec = Specification.where(entityTypeMatches(req.getEntityType()))
                .and(actorIdMatches(req.getActorId()))
                .and(actionMatches(req.getAction()))
                .and(createdAfter(req.getFrom()))
                .and(createdBefore(req.getTo()));
        return PagedResponse.of(auditLogRepository.findAll(spec, pageable).map(auditMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public AuditLogResponse getById(UUID id) {
        return auditLogRepository.findById(id)
                .map(auditMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("AuditLog", id));
    }

    private static Specification<AuditLog> entityTypeMatches(String entityType) {
        if (entityType == null || entityType.isBlank()) return null;
        return (root, query, cb) -> cb.equal(root.get("entityType"), entityType);
    }

    private static Specification<AuditLog> actorIdMatches(UUID actorId) {
        if (actorId == null) return null;
        return (root, query, cb) -> cb.equal(root.get("actorId"), actorId);
    }

    private static Specification<AuditLog> actionMatches(AuditAction action) {
        if (action == null) return null;
        return (root, query, cb) -> cb.equal(root.get("action"), action);
    }

    private static Specification<AuditLog> createdAfter(OffsetDateTime from) {
        if (from == null) return null;
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    private static Specification<AuditLog> createdBefore(OffsetDateTime to) {
        if (to == null) return null;
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to);
    }
}
