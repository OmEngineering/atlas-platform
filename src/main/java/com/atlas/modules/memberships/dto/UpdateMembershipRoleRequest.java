package com.atlas.modules.memberships.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateMembershipRoleRequest(
        @NotBlank String role
) {
}
