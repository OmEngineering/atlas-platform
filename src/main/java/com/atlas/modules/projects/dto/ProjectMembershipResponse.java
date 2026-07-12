package com.atlas.modules.projects.dto;

import java.time.Instant;
import java.util.UUID;

public record ProjectMembershipResponse(
        UUID id,
        UUID projectId,
        UUID userId,
        String role,
        Instant addedAt
) {
}
