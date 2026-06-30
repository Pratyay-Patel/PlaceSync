package com.placesync.common.audit;

import com.placesync.common.audit.service.AuditLogService;
import com.placesync.common.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

    private final AuditLogService auditLogService;

    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint pjp, Auditable auditable) throws Throwable {
        Object result = pjp.proceed();
        try {
            UserPrincipal principal = resolvePrincipal();
            UUID entityId = extractEntityId(result, auditable, pjp.getArgs());
            AuditLog entry = AuditLog.builder()
                    .entityType(auditable.entityType())
                    .entityId(entityId)
                    .action(auditable.action())
                    .actorId(principal != null ? principal.getId() : null)
                    .actorRole(principal != null ? principal.getRole().name() : null)
                    .actorEmail(principal != null ? principal.getEmail() : null)
                    .build();
            auditLogService.saveAsync(entry);
        } catch (Exception e) {
            log.warn("AuditAspect failed to build audit entry for {}.{}: {}",
                    pjp.getTarget().getClass().getSimpleName(),
                    pjp.getSignature().getName(), e.getMessage());
        }
        return result;
    }

    private UserPrincipal resolvePrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal;
        }
        return null;
    }

    private UUID extractEntityId(Object result, Auditable auditable, Object[] args) {
        if (auditable.entityIdParamIndex() >= 0 && args.length > auditable.entityIdParamIndex()) {
            Object param = args[auditable.entityIdParamIndex()];
            if (param instanceof UUID uuid) return uuid;
        }
        if (result != null) {
            try {
                return (UUID) result.getClass().getMethod("getId").invoke(result);
            } catch (Exception ignored) {
                // DTO does not expose getId() — entityId will be null
            }
        }
        return null;
    }
}
