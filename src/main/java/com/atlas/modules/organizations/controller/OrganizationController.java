package com.atlas.modules.organizations.controller;

import com.atlas.modules.auth.entity.User;
import com.atlas.modules.organizations.dto.CreateOrganizationRequest;
import com.atlas.modules.organizations.dto.MembershipResponse;
import com.atlas.modules.organizations.dto.OrganizationResponse;
import com.atlas.modules.organizations.dto.UpdateOrganizationRequest;
import com.atlas.modules.organizations.service.OrganizationService;
import com.atlas.shared.dto.ApiResponse;
import com.atlas.shared.dto.PageResponse;
import com.atlas.shared.security.SecurityContextAccessor;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations")
@Tag(name = "Organizations", description = "Organization and membership management")
public class OrganizationController {

    private final OrganizationService organizationService;
    private final SecurityContextAccessor securityContextAccessor;

    public OrganizationController(
            OrganizationService organizationService,
            SecurityContextAccessor securityContextAccessor) {
        this.organizationService = organizationService;
        this.securityContextAccessor = securityContextAccessor;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create organization")
    public ApiResponse<OrganizationResponse> create(@Valid @RequestBody CreateOrganizationRequest request) {
        User user = securityContextAccessor.requireVerifiedUser();
        return ApiResponse.of(organizationService.createOrganization(request, user));
    }

    @GetMapping("/{organizationId}")
    @Operation(summary = "Get organization by id")
    public ApiResponse<OrganizationResponse> get(@PathVariable UUID organizationId) {
        UUID userId = securityContextAccessor.currentUserId();
        return ApiResponse.of(organizationService.getOrganization(organizationId, userId));
    }

    @PatchMapping("/{organizationId}")
    @Operation(summary = "Update organization")
    public ApiResponse<OrganizationResponse> update(
            @PathVariable UUID organizationId,
            @Valid @RequestBody UpdateOrganizationRequest request) {
        UUID userId = securityContextAccessor.currentUserId();
        return ApiResponse.of(organizationService.updateOrganization(organizationId, userId, request));
    }

    @DeleteMapping("/{organizationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Archive organization")
    public void archive(@PathVariable UUID organizationId) {
        UUID userId = securityContextAccessor.currentUserId();
        organizationService.archiveOrganization(organizationId, userId);
    }

    @GetMapping("/{organizationId}/members")
    @Operation(summary = "List organization members")
    public ApiResponse<PageResponse<MembershipResponse>> listMembers(
            @PathVariable UUID organizationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        UUID userId = securityContextAccessor.currentUserId();
        return ApiResponse.of(organizationService.listMembers(organizationId, userId, page, pageSize));
    }
}
