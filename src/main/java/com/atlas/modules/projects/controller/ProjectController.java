package com.atlas.modules.projects.controller;

import com.atlas.modules.projects.dto.AddProjectMemberRequest;
import com.atlas.modules.projects.dto.ProjectMembershipResponse;
import com.atlas.modules.projects.dto.ProjectResponse;
import com.atlas.modules.projects.dto.UpdateProjectRequest;
import com.atlas.modules.projects.service.ProjectService;
import com.atlas.shared.dto.ApiResponse;
import com.atlas.shared.security.SecurityContextAccessor;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects")
@Tag(name = "Projects")
public class ProjectController {

    private final ProjectService projectService;
    private final SecurityContextAccessor securityContextAccessor;

    public ProjectController(ProjectService projectService, SecurityContextAccessor securityContextAccessor) {
        this.projectService = projectService;
        this.securityContextAccessor = securityContextAccessor;
    }

    @GetMapping("/{projectId}")
    public ApiResponse<ProjectResponse> get(@PathVariable UUID projectId) {
        return ApiResponse.of(projectService.get(projectId, securityContextAccessor.currentUserId()));
    }

    @PatchMapping("/{projectId}")
    public ApiResponse<ProjectResponse> update(
            @PathVariable UUID projectId,
            @Valid @RequestBody UpdateProjectRequest request) {
        return ApiResponse.of(projectService.update(
                projectId, securityContextAccessor.currentUserId(), request));
    }

    @DeleteMapping("/{projectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@PathVariable UUID projectId) {
        projectService.archive(projectId, securityContextAccessor.currentUserId());
    }

    @PostMapping("/{projectId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProjectMembershipResponse> addMember(
            @PathVariable UUID projectId,
            @Valid @RequestBody AddProjectMemberRequest request) {
        return ApiResponse.of(projectService.addMember(
                projectId, securityContextAccessor.currentUserId(), request));
    }

    @DeleteMapping("/{projectId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable UUID projectId, @PathVariable UUID userId) {
        projectService.removeMember(projectId, userId, securityContextAccessor.currentUserId());
    }
}
