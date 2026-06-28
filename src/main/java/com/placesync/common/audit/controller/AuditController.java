package com.placesync.common.audit.controller;

import com.placesync.common.audit.dto.AuditLogResponse;
import com.placesync.common.audit.dto.AuditSearchRequest;
import com.placesync.common.audit.service.AuditLogService;
import com.placesync.common.util.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/audit-log")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Audit", description = "Audit log — admin only")
public class AuditController {

    private final AuditLogService auditLogService;

    @GetMapping
    @Operation(summary = "Search audit log (paginated, filtered)")
    public ResponseEntity<PagedResponse<AuditLogResponse>> search(
            @ParameterObject AuditSearchRequest req,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(auditLogService.search(req, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get single audit log entry")
    public ResponseEntity<AuditLogResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(auditLogService.getById(id));
    }
}
