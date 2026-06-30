package com.placesync.common.audit;

import com.placesync.common.audit.service.AuditLogService;
import com.placesync.common.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
@RequiredArgsConstructor
public class AuthEventAuditListener {

    private final AuditLogService auditLogService;

    @EventListener
    public void onLoginSuccess(AuthenticationSuccessEvent event) {
        if (!(event.getAuthentication().getPrincipal() instanceof UserPrincipal principal)) return;
        auditLogService.saveAsync(AuditLog.builder()
                .entityType("User")
                .entityId(principal.getId())
                .action(AuditAction.LOGIN_SUCCESS)
                .actorId(principal.getId())
                .actorRole(principal.getRole().name())
                .actorEmail(principal.getEmail())
                .ipAddress(getClientIp())
                .build());
    }

    @EventListener
    public void onLoginFailure(AbstractAuthenticationFailureEvent event) {
        String attemptedEmail = event.getAuthentication().getName();
        auditLogService.saveAsync(AuditLog.builder()
                .entityType("User")
                .action(AuditAction.LOGIN_FAILURE)
                .actorEmail(attemptedEmail)
                .ipAddress(getClientIp())
                .build());
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            return attrs.getRequest().getRemoteAddr();
        } catch (IllegalStateException e) {
            return null;
        }
    }
}
