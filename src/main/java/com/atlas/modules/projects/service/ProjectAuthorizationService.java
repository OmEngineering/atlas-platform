package com.atlas.modules.projects.service;

import com.atlas.exception.ForbiddenException;
import com.atlas.exception.ResourceNotFoundException;
import com.atlas.modules.organizations.entity.Membership;
import com.atlas.modules.organizations.entity.MembershipRole;
import com.atlas.modules.organizations.entity.MembershipStatus;
import com.atlas.modules.organizations.repository.MembershipRepository;
import com.atlas.modules.organizations.service.MembershipAuthorizationService;
import com.atlas.modules.projects.entity.Project;
import com.atlas.modules.projects.entity.ProjectMembership;
import com.atlas.modules.projects.entity.ProjectMembershipRole;
import com.atlas.modules.projects.entity.ProjectVisibility;
import com.atlas.modules.projects.repository.ProjectMembershipRepository;
import com.atlas.modules.projects.repository.ProjectRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ProjectAuthorizationService {

    private final ProjectRepository projectRepository;
    private final ProjectMembershipRepository projectMembershipRepository;
    private final MembershipRepository membershipRepository;
    private final MembershipAuthorizationService membershipAuthorizationService;

    public ProjectAuthorizationService(
            ProjectRepository projectRepository,
            ProjectMembershipRepository projectMembershipRepository,
            MembershipRepository membershipRepository,
            MembershipAuthorizationService membershipAuthorizationService) {
        this.projectRepository = projectRepository;
        this.projectMembershipRepository = projectMembershipRepository;
        this.membershipRepository = membershipRepository;
        this.membershipAuthorizationService = membershipAuthorizationService;
    }

    public Project requireVisibleProject(UUID projectId, UUID userId) {
        Project project = projectRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        membershipRepository.findByOrganizationIdAndUserIdAndStatus(
                project.getOrganizationId(), userId, MembershipStatus.ACTIVE)
                .orElseThrow(() -> new ForbiddenException("You do not have access to this organization"));

        if (project.getVisibility() == ProjectVisibility.RESTRICTED
                && projectMembershipRepository.findByProjectIdAndUserId(projectId, userId).isEmpty()) {
            throw new ForbiddenException("You do not have access to this project");
        }
        return project;
    }

    public void requireProjectManager(UUID projectId, UUID userId) {
        Project project = requireVisibleProject(projectId, userId);
        if (isOrgAdmin(project.getOrganizationId(), userId)) {
            return;
        }
        ProjectMembership membership = projectMembershipRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ForbiddenException("Insufficient permissions for this operation"));
        if (!membership.getRole().isAtLeast(ProjectMembershipRole.PM)) {
            throw new ForbiddenException("Insufficient permissions for this operation");
        }
    }

    private boolean isOrgAdmin(UUID organizationId, UUID userId) {
        return membershipRepository.findByOrganizationIdAndUserIdAndStatus(
                        organizationId, userId, MembershipStatus.ACTIVE)
                .map(Membership::getRole)
                .map(role -> role.isAtLeast(MembershipRole.ADMIN))
                .orElse(false);
    }
}
