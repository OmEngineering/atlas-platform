package com.atlas.modules.notifications.service;

import com.atlas.exception.ResourceNotFoundException;
import com.atlas.modules.notifications.dto.NotificationPreferenceResponse;
import com.atlas.modules.notifications.dto.NotificationResponse;
import com.atlas.modules.notifications.dto.UnreadCountResponse;
import com.atlas.modules.notifications.dto.UpdateNotificationPreferenceRequest;
import com.atlas.modules.notifications.entity.Notification;
import com.atlas.modules.notifications.entity.NotificationPreference;
import com.atlas.modules.notifications.mapper.NotificationMapper;
import com.atlas.modules.notifications.repository.NotificationPreferenceRepository;
import com.atlas.modules.notifications.repository.NotificationRepository;
import com.atlas.shared.dto.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;

    public NotificationService(
            NotificationRepository notificationRepository,
            NotificationPreferenceRepository preferenceRepository) {
        this.notificationRepository = notificationRepository;
        this.preferenceRepository = preferenceRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(UUID userId, boolean unreadOnly, int page, int pageSize) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(pageSize, 1), 100);
        PageRequest pageable = PageRequest.of(safePage, safeSize);
        Page<Notification> result = unreadOnly
                ? notificationRepository.findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(userId, pageable)
                : notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return new PageResponse<>(
                result.getContent().stream().map(NotificationMapper::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse unreadCount(UUID userId) {
        return new UnreadCountResponse(notificationRepository.countByUserIdAndReadAtIsNull(userId));
    }

    @Transactional
    public NotificationResponse markRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (notification.getReadAt() == null) {
            notification.setReadAt(Instant.now());
            notification = notificationRepository.save(notification);
        }
        return NotificationMapper.toResponse(notification);
    }

    @Transactional
    public void markAllRead(UUID userId) {
        notificationRepository.markAllRead(userId, Instant.now());
    }

    @Transactional(readOnly = true)
    public NotificationPreferenceResponse getPreferences(UUID userId) {
        return NotificationMapper.toPreferenceResponse(getOrCreatePreferences(userId));
    }

    @Transactional
    public NotificationPreferenceResponse updatePreferences(UUID userId, UpdateNotificationPreferenceRequest request) {
        NotificationPreference preference = getOrCreatePreferences(userId);
        preference.setInAppEnabled(Boolean.TRUE.equals(request.inAppEnabled()));
        preference.setEmailEnabled(Boolean.TRUE.equals(request.emailEnabled()));
        return NotificationMapper.toPreferenceResponse(preferenceRepository.save(preference));
    }

    @Transactional
    public NotificationPreference getOrCreatePreferences(UUID userId) {
        return preferenceRepository.findByUserId(userId).orElseGet(() -> {
            NotificationPreference created = new NotificationPreference();
            created.setUserId(userId);
            created.setInAppEnabled(true);
            created.setEmailEnabled(true);
            return preferenceRepository.save(created);
        });
    }
}
