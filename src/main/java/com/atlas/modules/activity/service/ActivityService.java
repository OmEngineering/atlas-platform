package com.atlas.modules.activity.service;

import com.atlas.exception.ApiException;
import com.atlas.exception.ErrorCode;
import com.atlas.modules.activity.dto.ActivityEntryResponse;
import com.atlas.modules.activity.entity.ActivityEntry;
import com.atlas.modules.activity.entity.ActivityTargetType;
import com.atlas.modules.activity.mapper.ActivityMapper;
import com.atlas.modules.activity.repository.ActivityEntryRepository;
import com.atlas.modules.organizations.service.MembershipAuthorizationService;
import com.atlas.shared.dto.CursorPageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ActivityService {

    private final ActivityEntryRepository activityEntryRepository;
    private final MembershipAuthorizationService membershipAuthorizationService;

    public ActivityService(
            ActivityEntryRepository activityEntryRepository,
            MembershipAuthorizationService membershipAuthorizationService) {
        this.activityEntryRepository = activityEntryRepository;
        this.membershipAuthorizationService = membershipAuthorizationService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
            UUID organizationId,
            UUID projectId,
            UUID actorId,
            String eventType,
            String summary,
            ActivityTargetType targetType,
            UUID targetId,
            Map<String, Object> metadata) {
        ActivityEntry entry = new ActivityEntry();
        entry.setOrganizationId(organizationId);
        entry.setProjectId(projectId);
        entry.setActorId(actorId);
        entry.setEventType(eventType);
        entry.setSummary(summary);
        entry.setTargetType(targetType);
        entry.setTargetId(targetId);
        entry.setMetadata(metadata == null ? Map.of() : new HashMap<>(metadata));
        entry.setCreatedAt(Instant.now());
        activityEntryRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public CursorPageResponse<ActivityEntryResponse> listForOrganization(
            UUID organizationId,
            UUID userId,
            UUID projectId,
            ActivityTargetType targetType,
            UUID targetId,
            String cursor,
            int pageSize) {
        membershipAuthorizationService.requireAnyActiveMembership(userId, organizationId);

        int limit = Math.min(Math.max(pageSize, 1), 100);
        Instant cursorCreatedAt = null;
        UUID cursorId = null;
        if (cursor != null && !cursor.isBlank()) {
            String[] parts = cursor.split("\\|", 2);
            if (parts.length != 2) {
                throw new ApiException(ErrorCode.VALIDATION_ERROR, "Invalid cursor", HttpStatus.UNPROCESSABLE_ENTITY);
            }
            try {
                cursorCreatedAt = Instant.parse(parts[0]);
                cursorId = UUID.fromString(parts[1]);
            } catch (Exception ex) {
                throw new ApiException(ErrorCode.VALIDATION_ERROR, "Invalid cursor", HttpStatus.UNPROCESSABLE_ENTITY);
            }
        }

        List<ActivityEntry> rows = activityEntryRepository.findPage(
                organizationId,
                projectId,
                targetType,
                targetId,
                cursorCreatedAt,
                cursorId,
                PageRequest.of(0, limit + 1));

        boolean hasMore = rows.size() > limit;
        List<ActivityEntry> page = hasMore ? rows.subList(0, limit) : rows;
        List<ActivityEntryResponse> items = page.stream().map(ActivityMapper::toResponse).toList();

        String nextCursor = null;
        if (hasMore && !page.isEmpty()) {
            ActivityEntry last = page.get(page.size() - 1);
            nextCursor = last.getCreatedAt().toString() + "|" + last.getId();
        }

        return new CursorPageResponse<>(items, nextCursor, hasMore);
    }
}
