package com.atlas.modules.activity.controller;

import com.atlas.modules.activity.dto.ActivityEntryResponse;
import com.atlas.modules.activity.entity.ActivityTargetType;
import com.atlas.modules.activity.service.ActivityService;
import com.atlas.shared.dto.ApiResponse;
import com.atlas.shared.dto.CursorPageResponse;
import com.atlas.shared.security.SecurityContextAccessor;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/activity")
@Tag(name = "Activity")
public class OrganizationActivityController {

    private final ActivityService activityService;
    private final SecurityContextAccessor securityContextAccessor;

    public OrganizationActivityController(
            ActivityService activityService,
            SecurityContextAccessor securityContextAccessor) {
        this.activityService = activityService;
        this.securityContextAccessor = securityContextAccessor;
    }

    @GetMapping
    public ApiResponse<CursorPageResponse<ActivityEntryResponse>> list(
            @PathVariable UUID organizationId,
            @RequestParam(required = false) UUID projectId,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) UUID targetId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int pageSize) {
        ActivityTargetType parsedTargetType = null;
        if (targetType != null && !targetType.isBlank()) {
            parsedTargetType = ActivityTargetType.valueOf(targetType.trim().toUpperCase());
        }
        return ApiResponse.of(activityService.listForOrganization(
                organizationId,
                securityContextAccessor.currentUserId(),
                projectId,
                parsedTargetType,
                targetId,
                cursor,
                pageSize));
    }
}
