package com.atlas.modules.audit.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        UUID organizationId,
        UUID actorId,
        String actorType,
        String action,
        String targetType,
        UUID targetId,
        Map<String, Object> metadata,
        Instant createdAt
) {
}
