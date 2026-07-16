package com.atlas.modules.labels.controller;

import com.atlas.modules.labels.dto.CreateLabelRequest;
import com.atlas.modules.labels.dto.LabelResponse;
import com.atlas.modules.labels.service.LabelService;
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
@RequestMapping("/api/v1/projects/{projectId}/labels")
@Tag(name = "Labels")
public class ProjectLabelController {

    private final LabelService labelService;
    private final SecurityContextAccessor securityContextAccessor;

    public ProjectLabelController(
            LabelService labelService,
            SecurityContextAccessor securityContextAccessor) {
        this.labelService = labelService;
        this.securityContextAccessor = securityContextAccessor;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<LabelResponse> create(
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateLabelRequest request) {
        return ApiResponse.of(labelService.create(
                projectId, securityContextAccessor.currentUserId(), request));
    }

    @GetMapping
    public ApiResponse<List<LabelResponse>> list(@PathVariable UUID projectId) {
        return ApiResponse.of(labelService.listForProject(
                projectId, securityContextAccessor.currentUserId()));
    }
}
