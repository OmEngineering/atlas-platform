package com.atlas.modules.organizations.dto;

import java.time.Instant;
import java.util.UUID;

public record MembershipResponse(
        UUID id,
        UUID userId,
        String role,
        String status,
        Instant joinedAt
) {
}
