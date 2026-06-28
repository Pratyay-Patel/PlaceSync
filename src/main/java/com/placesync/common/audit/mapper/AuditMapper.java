package com.placesync.common.audit.mapper;

import com.placesync.common.audit.AuditLog;
import com.placesync.common.audit.dto.AuditLogResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuditMapper {
    AuditLogResponse toResponse(AuditLog auditLog);
}
