package com.atlas.modules.notifications.dto;

public record NotificationPreferenceResponse(
        boolean inAppEnabled,
        boolean emailEnabled
) {
}
