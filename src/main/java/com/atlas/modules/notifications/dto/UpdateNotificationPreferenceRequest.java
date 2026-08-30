package com.atlas.modules.notifications.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateNotificationPreferenceRequest(
        @NotNull Boolean inAppEnabled,
        @NotNull Boolean emailEnabled
) {
}
