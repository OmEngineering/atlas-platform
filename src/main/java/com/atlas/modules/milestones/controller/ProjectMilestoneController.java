package com.atlas.modules.milestones.controller;

import com.atlas.modules.milestones.dto.CreateMilestoneRequest;
import com.atlas.modules.milestones.dto.MilestoneResponse;
import com.atlas.modules.milestones.service.MilestoneService;
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
@RequestMapping("/api/v1/projects/{projectId}/milestones")
@Tag(name = "Milestones")
public class ProjectMilestoneController {

    private final MilestoneService milestoneService;
    private final SecurityContextAccessor securityContextAccessor;

    public ProjectMilestoneController(
            MilestoneService milestoneService,
            SecurityContextAccessor securityContextAccessor) {
        this.milestoneService = milestoneService;
        this.securityContextAccessor = securityContextAccessor;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MilestoneResponse> create(
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateMilestoneRequest request) {
        return ApiResponse.of(milestoneService.create(
                projectId, securityContextAccessor.currentUserId(), request));
    }

    @GetMapping
    public ApiResponse<List<MilestoneResponse>> list(@PathVariable UUID projectId) {
        return ApiResponse.of(milestoneService.listForProject(
                projectId, securityContextAccessor.currentUserId()));
    }
}
