package com.atlas.modules.notifications.service;

import com.atlas.modules.auth.entity.User;
import com.atlas.modules.auth.repository.UserRepository;
import com.atlas.modules.notifications.entity.EmailDeliveryStatus;
import com.atlas.modules.notifications.entity.Notification;
import com.atlas.modules.notifications.entity.NotificationPreference;
import com.atlas.modules.notifications.entity.NotificationTargetType;
import com.atlas.modules.notifications.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class NotificationDispatcher {

    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final EmailNotificationSender emailNotificationSender;

    public NotificationDispatcher(
            NotificationRepository notificationRepository,
            NotificationService notificationService,
            UserRepository userRepository,
            EmailNotificationSender emailNotificationSender) {
        this.notificationRepository = notificationRepository;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.emailNotificationSender = emailNotificationSender;
    }

    @Transactional
    public void dispatch(
            UUID recipientUserId,
            UUID organizationId,
            String eventType,
            String title,
            String body,
            NotificationTargetType targetType,
            UUID targetId) {
        if (recipientUserId == null) {
            return;
        }

        NotificationPreference preference = notificationService.getOrCreatePreferences(recipientUserId);
        if (!preference.isInAppEnabled() && !preference.isEmailEnabled()) {
            return;
        }

        Notification notification = new Notification();
        notification.setUserId(recipientUserId);
        notification.setOrganizationId(organizationId);
        notification.setEventType(eventType);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setTargetType(targetType);
        notification.setTargetId(targetId);
        notification.setCreatedAt(Instant.now());

        if (!preference.isInAppEnabled()) {
            // Still persist for audit of email attempts when in-app is off but email on —
            // mark immediately read so inbox stays empty.
            notification.setReadAt(Instant.now());
        }

        if (preference.isEmailEnabled()) {
            User user = userRepository.findById(recipientUserId).orElse(null);
            if (user != null && emailNotificationSender.send(user.getId(), user.getEmail(), title, body)) {
                notification.setEmailStatus(EmailDeliveryStatus.SENT);
                notification.setEmailSentAt(Instant.now());
            } else {
                notification.setEmailStatus(EmailDeliveryStatus.FAILED);
            }
        } else {
            notification.setEmailStatus(EmailDeliveryStatus.SKIPPED);
        }

        if (preference.isInAppEnabled() || preference.isEmailEnabled()) {
            notificationRepository.save(notification);
        }
    }
}
