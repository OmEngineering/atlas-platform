package com.atlas.modules.notifications.controller;

import com.atlas.modules.notifications.dto.NotificationPreferenceResponse;
import com.atlas.modules.notifications.dto.NotificationResponse;
import com.atlas.modules.notifications.dto.UnreadCountResponse;
import com.atlas.modules.notifications.dto.UpdateNotificationPreferenceRequest;
import com.atlas.modules.notifications.service.NotificationService;
import com.atlas.shared.dto.ApiResponse;
import com.atlas.shared.dto.PageResponse;
import com.atlas.shared.security.SecurityContextAccessor;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final SecurityContextAccessor securityContextAccessor;

    public NotificationController(
            NotificationService notificationService,
            SecurityContextAccessor securityContextAccessor) {
        this.notificationService = notificationService;
        this.securityContextAccessor = securityContextAccessor;
    }

    @GetMapping
    public ApiResponse<PageResponse<NotificationResponse>> list(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.of(notificationService.list(
                securityContextAccessor.currentUserId(), unreadOnly, page, pageSize));
    }

    @GetMapping("/unread-count")
    public ApiResponse<UnreadCountResponse> unreadCount() {
        return ApiResponse.of(notificationService.unreadCount(securityContextAccessor.currentUserId()));
    }

    @PostMapping("/{notificationId}/read")
    public ApiResponse<NotificationResponse> markRead(@PathVariable UUID notificationId) {
        return ApiResponse.of(notificationService.markRead(
                notificationId, securityContextAccessor.currentUserId()));
    }

    @PostMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllRead() {
        notificationService.markAllRead(securityContextAccessor.currentUserId());
    }

    @GetMapping("/preferences")
    public ApiResponse<NotificationPreferenceResponse> getPreferences() {
        return ApiResponse.of(notificationService.getPreferences(securityContextAccessor.currentUserId()));
    }

    @PutMapping("/preferences")
    public ApiResponse<NotificationPreferenceResponse> updatePreferences(
            @Valid @RequestBody UpdateNotificationPreferenceRequest request) {
        return ApiResponse.of(notificationService.updatePreferences(
                securityContextAccessor.currentUserId(), request));
    }
}
