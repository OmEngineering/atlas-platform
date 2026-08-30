package com.atlas.modules.projects.service;

import com.atlas.exception.ApiException;
import com.atlas.exception.ErrorCode;
import com.atlas.exception.ResourceNotFoundException;
import com.atlas.modules.auth.entity.User;
import com.atlas.modules.organizations.entity.MembershipRole;
import com.atlas.modules.organizations.entity.MembershipStatus;
import com.atlas.modules.organizations.repository.MembershipRepository;
import com.atlas.modules.organizations.service.MembershipAuthorizationService;
import com.atlas.modules.projects.dto.AddProjectMemberRequest;
import com.atlas.modules.projects.dto.CreateProjectRequest;
import com.atlas.modules.projects.dto.ProjectMembershipResponse;
import com.atlas.modules.projects.dto.ProjectResponse;
import com.atlas.modules.projects.dto.UpdateProjectRequest;
import com.atlas.modules.projects.entity.Project;
import com.atlas.modules.projects.entity.ProjectMembership;
import com.atlas.modules.projects.entity.ProjectMembershipRole;
import com.atlas.modules.projects.entity.ProjectStatus;
import com.atlas.modules.projects.entity.ProjectVisibility;
import com.atlas.modules.projects.mapper.ProjectMapper;
import com.atlas.modules.projects.repository.ProjectMembershipRepository;
import com.atlas.modules.projects.repository.ProjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMembershipRepository projectMembershipRepository;
    private final MembershipRepository membershipRepository;
    private final MembershipAuthorizationService membershipAuthorizationService;
    private final ProjectAuthorizationService projectAuthorizationService;

    public ProjectService(
            ProjectRepository projectRepository,
            ProjectMembershipRepository projectMembershipRepository,
            MembershipRepository membershipRepository,
            MembershipAuthorizationService membershipAuthorizationService,
            ProjectAuthorizationService projectAuthorizationService) {
        this.projectRepository = projectRepository;
        this.projectMembershipRepository = projectMembershipRepository;
        this.membershipRepository = membershipRepository;
        this.membershipAuthorizationService = membershipAuthorizationService;
        this.projectAuthorizationService = projectAuthorizationService;
    }

    @Transactional
    public ProjectResponse create(UUID organizationId, User creator, CreateProjectRequest request) {
        membershipAuthorizationService.requireActiveMembership(organizationId, creator.getId(), MembershipRole.ADMIN);

        String key = request.key().trim().toUpperCase(Locale.ROOT);
        if (projectRepository.existsByOrganizationIdAndKeyIgnoreCase(organizationId, key)) {
            throw new ApiException(ErrorCode.PROJECT_KEY_TAKEN, "Project key is already taken", HttpStatus.CONFLICT);
        }

        Project project = new Project();
        project.setOrganizationId(organizationId);
        project.setName(request.name().trim());
        project.setKey(key);
        project.setDescription(request.description());
        project.setVisibility(parseVisibility(request.visibility()));
        project.setStatus(ProjectStatus.ACTIVE);
        project.setCreatedBy(creator.getId());
        project = projectRepository.save(project);

        addMembership(project.getId(), creator.getId(), ProjectMembershipRole.PM, creator.getId());
        return ProjectMapper.toResponse(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> listForOrganization(UUID organizationId, UUID userId) {
        membershipAuthorizationService.requireAnyActiveMembership(userId, organizationId);
        return projectRepository.findVisibleForUser(organizationId, userId, ProjectStatus.ACTIVE).stream()
                .map(ProjectMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(UUID projectId, UUID userId) {
        return ProjectMapper.toResponse(projectAuthorizationService.requireVisibleProject(projectId, userId));
    }

    @Transactional
    public ProjectResponse update(UUID projectId, UUID userId, UpdateProjectRequest request) {
        projectAuthorizationService.requireProjectManager(projectId, userId);
        Project project = findActiveProject(projectId);

        if (request.name() != null) {
            project.setName(request.name().trim());
        }
        if (request.description() != null) {
            project.setDescription(request.description());
        }
        if (request.visibility() != null) {
            project.setVisibility(parseVisibility(request.visibility()));
        }

        return ProjectMapper.toResponse(projectRepository.save(project));
    }

    @Transactional
    public void archive(UUID projectId, UUID userId) {
        projectAuthorizationService.requireProjectManager(projectId, userId);
        Project project = findActiveProject(projectId);
        project.setStatus(ProjectStatus.ARCHIVED);
        project.setDeletedAt(Instant.now());
        projectRepository.save(project);
    }

    @Transactional
    public ProjectMembershipResponse addMember(UUID projectId, UUID actorId, AddProjectMemberRequest request) {
        projectAuthorizationService.requireProjectManager(projectId, actorId);
        Project project = findActiveProject(projectId);

        membershipRepository.findByOrganizationIdAndUserIdAndStatus(
                        project.getOrganizationId(), request.userId(), MembershipStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(
                        ErrorCode.VALIDATION_ERROR, "User is not an active org member", HttpStatus.UNPROCESSABLE_ENTITY));

        if (projectMembershipRepository.existsByProjectIdAndUserId(projectId, request.userId())) {
            throw new ApiException(ErrorCode.ALREADY_MEMBER, "User is already on this project", HttpStatus.CONFLICT);
        }

        ProjectMembershipRole role = parseProjectRole(request.role());
        ProjectMembership membership = addMembership(projectId, request.userId(), role, actorId);
        return ProjectMapper.toMembershipResponse(membership);
    }

    @Transactional
    public void removeMember(UUID projectId, UUID targetUserId, UUID actorId) {
        projectAuthorizationService.requireProjectManager(projectId, actorId);
        findActiveProject(projectId);

        ProjectMembership membership = projectMembershipRepository.findByProjectIdAndUserId(projectId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Project member not found"));

        if (membership.getRole() == ProjectMembershipRole.PM
                && projectMembershipRepository.findByProjectId(projectId).stream()
                        .filter(m -> m.getRole() == ProjectMembershipRole.PM)
                        .count() <= 1) {
            throw new ApiException(ErrorCode.CONFLICT, "Project must keep at least one PM", HttpStatus.CONFLICT);
        }

        projectMembershipRepository.delete(membership);
    }

    private Project findActiveProject(UUID projectId) {
        Project project = projectRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        if (project.getStatus() != ProjectStatus.ACTIVE) {
            throw new ResourceNotFoundException("Project not found");
        }
        return project;
    }

    private ProjectMembership addMembership(
            UUID projectId, UUID userId, ProjectMembershipRole role, UUID addedBy) {
        ProjectMembership membership = new ProjectMembership();
        membership.setProjectId(projectId);
        membership.setUserId(userId);
        membership.setRole(role);
        membership.setAddedBy(addedBy);
        membership.setAddedAt(Instant.now());
        return projectMembershipRepository.save(membership);
    }

    private ProjectVisibility parseVisibility(String visibility) {
        if (visibility == null || visibility.isBlank()) {
            return ProjectVisibility.ORG_WIDE;
        }
        try {
            return ProjectVisibility.valueOf(visibility.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Invalid visibility", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private ProjectMembershipRole parseProjectRole(String role) {
        try {
            return ProjectMembershipRole.valueOf(role.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Invalid project role", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }
}
