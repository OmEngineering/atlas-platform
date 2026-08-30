package com.atlas.modules.invitations.controller;

import com.atlas.modules.invitations.service.InvitationService;
import com.atlas.modules.organizations.dto.MembershipResponse;
import com.atlas.modules.organizations.mapper.OrganizationMapper;
import com.atlas.shared.dto.ApiResponse;
import com.atlas.shared.security.SecurityContextAccessor;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invitations")
@Tag(name = "Invitations")
public class InvitationController {

    private final InvitationService invitationService;
    private final SecurityContextAccessor securityContextAccessor;

    public InvitationController(InvitationService invitationService, SecurityContextAccessor securityContextAccessor) {
        this.invitationService = invitationService;
        this.securityContextAccessor = securityContextAccessor;
    }

    @DeleteMapping("/{invitationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable UUID invitationId) {
        invitationService.revoke(invitationId, securityContextAccessor.currentUserId());
    }

    @PostMapping("/{invitationId}/accept")
    public ApiResponse<MembershipResponse> accept(@PathVariable UUID invitationId) {
        var membership = invitationService.accept(invitationId, securityContextAccessor.requireVerifiedUser());
        return ApiResponse.of(OrganizationMapper.toMembershipResponse(membership));
    }
}
