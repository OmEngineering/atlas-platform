package com.atlas.modules.invitations.dto;

import java.time.Instant;
import java.util.UUID;

public record InvitationResponse(
        UUID id,
        UUID organizationId,
        String email,
        String role,
        String status,
        Instant expiresAt,
        Instant createdAt,
        String inviteToken
) {
}
