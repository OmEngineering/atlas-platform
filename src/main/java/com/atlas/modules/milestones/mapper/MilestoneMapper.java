package com.atlas.modules.milestones.mapper;

import com.atlas.modules.milestones.dto.MilestoneResponse;
import com.atlas.modules.milestones.entity.Milestone;

public final class MilestoneMapper {

    private MilestoneMapper() {
    }

    public static MilestoneResponse toResponse(Milestone milestone) {
        return new MilestoneResponse(
                milestone.getId(),
                milestone.getProjectId(),
                milestone.getName(),
                milestone.getDescription(),
                milestone.getStartDate(),
                milestone.getDueDate(),
                milestone.getStatus().name(),
                milestone.getCreatedAt(),
                milestone.getUpdatedAt()
        );
    }
}
