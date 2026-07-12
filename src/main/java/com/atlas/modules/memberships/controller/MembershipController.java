package com.atlas.modules.memberships.controller;

import com.atlas.modules.memberships.dto.UpdateMembershipRoleRequest;
import com.atlas.modules.memberships.service.MembershipManagementService;
import com.atlas.modules.organizations.dto.MembershipResponse;
import com.atlas.shared.dto.ApiResponse;
import com.atlas.shared.security.SecurityContextAccessor;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/memberships")
@Tag(name = "Memberships")
public class MembershipController {

    private final MembershipManagementService membershipManagementService;
    private final SecurityContextAccessor securityContextAccessor;

    public MembershipController(
            MembershipManagementService membershipManagementService,
            SecurityContextAccessor securityContextAccessor) {
        this.membershipManagementService = membershipManagementService;
        this.securityContextAccessor = securityContextAccessor;
    }

    @PatchMapping("/{membershipId}")
    public ApiResponse<MembershipResponse> updateRole(
            @PathVariable UUID membershipId,
            @Valid @RequestBody UpdateMembershipRoleRequest request) {
        return ApiResponse.of(membershipManagementService.updateRole(
                membershipId, securityContextAccessor.currentUserId(), request.role()));
    }

    @DeleteMapping("/{membershipId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable UUID membershipId) {
        membershipManagementService.removeMember(membershipId, securityContextAccessor.currentUserId());
    }
}
