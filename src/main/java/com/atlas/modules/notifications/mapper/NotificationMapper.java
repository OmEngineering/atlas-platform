package com.atlas.modules.notifications.mapper;

import com.atlas.modules.notifications.dto.NotificationPreferenceResponse;
import com.atlas.modules.notifications.dto.NotificationResponse;
import com.atlas.modules.notifications.entity.Notification;
import com.atlas.modules.notifications.entity.NotificationPreference;

public final class NotificationMapper {

    private NotificationMapper() {
    }

    public static NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getOrganizationId(),
                notification.getEventType(),
                notification.getTitle(),
                notification.getBody(),
                notification.getTargetType().name(),
                notification.getTargetId(),
                notification.getReadAt(),
                notification.getEmailStatus().name(),
                notification.getCreatedAt()
        );
    }

    public static NotificationPreferenceResponse toPreferenceResponse(NotificationPreference preference) {
        return new NotificationPreferenceResponse(
                preference.isInAppEnabled(),
                preference.isEmailEnabled()
        );
    }
}
