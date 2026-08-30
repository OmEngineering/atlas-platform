package com.atlas.modules.notifications.dto;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID organizationId,
        String eventType,
        String title,
        String body,
        String targetType,
        UUID targetId,
        Instant readAt,
        String emailStatus,
        Instant createdAt
) {
}
