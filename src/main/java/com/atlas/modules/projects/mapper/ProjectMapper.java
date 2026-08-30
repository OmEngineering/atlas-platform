package com.atlas.modules.projects.mapper;

import com.atlas.modules.projects.dto.ProjectMembershipResponse;
import com.atlas.modules.projects.dto.ProjectResponse;
import com.atlas.modules.projects.entity.Project;
import com.atlas.modules.projects.entity.ProjectMembership;

public final class ProjectMapper {

    private ProjectMapper() {
    }

    public static ProjectResponse toResponse(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getOrganizationId(),
                project.getName(),
                project.getKey(),
                project.getDescription(),
                project.getStatus().name(),
                project.getVisibility().name(),
                project.getCreatedBy(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }

    public static ProjectMembershipResponse toMembershipResponse(ProjectMembership membership) {
        return new ProjectMembershipResponse(
                membership.getId(),
                membership.getProjectId(),
                membership.getUserId(),
                membership.getRole().name(),
                membership.getAddedAt()
        );
    }
}
