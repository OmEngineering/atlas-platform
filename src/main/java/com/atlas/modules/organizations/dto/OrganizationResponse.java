package com.atlas.modules.organizations.dto;

import java.time.Instant;
import java.util.UUID;

public record OrganizationResponse(
        UUID id,
        String name,
        String slug,
        String logoUrl,
        String description,
        String timezone,
        String country,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
