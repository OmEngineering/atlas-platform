package com.atlas.modules.projects.controller;

import com.atlas.modules.projects.dto.CreateProjectRequest;
import com.atlas.modules.projects.dto.ProjectResponse;
import com.atlas.modules.projects.service.ProjectService;
import com.atlas.shared.dto.ApiResponse;
import com.atlas.shared.security.SecurityContextAccessor;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/projects")
@Tag(name = "Organization Projects")
public class OrganizationProjectController {

    private final ProjectService projectService;
    private final SecurityContextAccessor securityContextAccessor;

    public OrganizationProjectController(ProjectService projectService, SecurityContextAccessor securityContextAccessor) {
        this.projectService = projectService;
        this.securityContextAccessor = securityContextAccessor;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProjectResponse> create(
            @PathVariable UUID organizationId,
            @Valid @RequestBody CreateProjectRequest request) {
        return ApiResponse.of(projectService.create(
                organizationId, securityContextAccessor.requireVerifiedUser(), request));
    }

    @GetMapping
    public ApiResponse<List<ProjectResponse>> list(@PathVariable UUID organizationId) {
        return ApiResponse.of(projectService.listForOrganization(
                organizationId, securityContextAccessor.currentUserId()));
    }
}
