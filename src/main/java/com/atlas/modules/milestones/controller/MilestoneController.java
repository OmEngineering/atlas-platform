package com.atlas.modules.milestones.controller;

import com.atlas.modules.milestones.dto.MilestoneResponse;
import com.atlas.modules.milestones.dto.UpdateMilestoneRequest;
import com.atlas.modules.milestones.service.MilestoneService;
import com.atlas.shared.dto.ApiResponse;
import com.atlas.shared.security.SecurityContextAccessor;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/milestones")
@Tag(name = "Milestones")
public class MilestoneController {

    private final MilestoneService milestoneService;
    private final SecurityContextAccessor securityContextAccessor;

    public MilestoneController(
            MilestoneService milestoneService,
            SecurityContextAccessor securityContextAccessor) {
        this.milestoneService = milestoneService;
        this.securityContextAccessor = securityContextAccessor;
    }

    @GetMapping("/{id}")
    public ApiResponse<MilestoneResponse> get(@PathVariable UUID id) {
        return ApiResponse.of(milestoneService.get(id, securityContextAccessor.currentUserId()));
    }

    @PatchMapping("/{id}")
    public ApiResponse<MilestoneResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMilestoneRequest request) {
        return ApiResponse.of(milestoneService.update(id, securityContextAccessor.currentUserId(), request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        milestoneService.delete(id, securityContextAccessor.currentUserId());
    }
}
