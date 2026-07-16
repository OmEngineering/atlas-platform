package com.atlas.modules.activity.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ActivityEntryResponse(
        UUID id,
        UUID organizationId,
        UUID projectId,
        UUID actorId,
        String eventType,
        String summary,
        String targetType,
        UUID targetId,
        Map<String, Object> metadata,
        Instant createdAt
) {
}
