package com.atlas.modules.projects.dto;

import java.time.Instant;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        UUID organizationId,
        String name,
        String key,
        String description,
        String status,
        String visibility,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt
) {
}
