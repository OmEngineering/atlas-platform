package com.atlas.modules.labels.mapper;

import com.atlas.modules.labels.dto.LabelResponse;
import com.atlas.modules.labels.entity.Label;

public final class LabelMapper {

    private LabelMapper() {
    }

    public static LabelResponse toResponse(Label label) {
        return new LabelResponse(
                label.getId(),
                label.getOrganizationId(),
                label.getProjectId(),
                label.getName(),
                label.getColor(),
                label.getCreatedAt()
        );
    }
}
