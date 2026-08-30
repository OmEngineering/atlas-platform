package com.atlas.modules.audit.mapper;

import com.atlas.modules.audit.dto.AuditLogResponse;
import com.atlas.modules.audit.entity.AuditLog;

public final class AuditLogMapper {

    private AuditLogMapper() {
    }

    public static AuditLogResponse toResponse(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getOrganizationId(),
                log.getActorId(),
                log.getActorType().name(),
                log.getAction(),
                log.getTargetType().name(),
                log.getTargetId(),
                log.getMetadata(),
                log.getCreatedAt()
        );
    }
}
