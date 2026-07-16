package com.atlas.modules.activity.mapper;

import com.atlas.modules.activity.dto.ActivityEntryResponse;
import com.atlas.modules.activity.entity.ActivityEntry;

public final class ActivityMapper {

    private ActivityMapper() {
    }

    public static ActivityEntryResponse toResponse(ActivityEntry entry) {
        return new ActivityEntryResponse(
                entry.getId(),
                entry.getOrganizationId(),
                entry.getProjectId(),
                entry.getActorId(),
                entry.getEventType(),
                entry.getSummary(),
                entry.getTargetType().name(),
                entry.getTargetId(),
                entry.getMetadata(),
                entry.getCreatedAt()
        );
    }
}
