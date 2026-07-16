package com.atlas.modules.labels.dto;

import java.time.Instant;
import java.util.UUID;

public record LabelResponse(
        UUID id,
        UUID organizationId,
        UUID projectId,
        String name,
        String color,
        Instant createdAt
) {
}
