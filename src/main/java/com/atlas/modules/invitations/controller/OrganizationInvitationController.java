package com.atlas.modules.invitations.controller;

import com.atlas.modules.invitations.dto.CreateInvitationRequest;
import com.atlas.modules.invitations.dto.InvitationResponse;
import com.atlas.modules.invitations.service.InvitationService;
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
@RequestMapping("/api/v1/organizations/{organizationId}/invitations")
@Tag(name = "Organization Invitations")
public class OrganizationInvitationController {

    private final InvitationService invitationService;
    private final SecurityContextAccessor securityContextAccessor;

    public OrganizationInvitationController(
            InvitationService invitationService,
            SecurityContextAccessor securityContextAccessor) {
        this.invitationService = invitationService;
        this.securityContextAccessor = securityContextAccessor;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<InvitationResponse> create(
            @PathVariable UUID organizationId,
            @Valid @RequestBody CreateInvitationRequest request) {
        return ApiResponse.of(invitationService.create(
                organizationId, securityContextAccessor.currentUserId(), request));
    }

    @GetMapping
    public ApiResponse<List<InvitationResponse>> listPending(@PathVariable UUID organizationId) {
        return ApiResponse.of(invitationService.listPending(organizationId, securityContextAccessor.currentUserId()));
    }
}
