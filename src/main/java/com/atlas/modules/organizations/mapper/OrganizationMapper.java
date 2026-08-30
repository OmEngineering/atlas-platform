package com.atlas.modules.organizations.mapper;

import com.atlas.modules.organizations.dto.MembershipResponse;
import com.atlas.modules.organizations.dto.OrganizationResponse;
import com.atlas.modules.organizations.entity.Membership;
import com.atlas.modules.organizations.entity.Organization;

public final class OrganizationMapper {

    private OrganizationMapper() {
    }

    public static OrganizationResponse toResponse(Organization organization) {
        return new OrganizationResponse(
                organization.getId(),
                organization.getName(),
                organization.getSlug(),
                organization.getLogoUrl(),
                organization.getDescription(),
                organization.getTimezone(),
                organization.getCountry(),
                organization.getStatus().name(),
                organization.getCreatedAt(),
                organization.getUpdatedAt()
        );
    }

    public static MembershipResponse toMembershipResponse(Membership membership) {
        return new MembershipResponse(
                membership.getId(),
                membership.getUserId(),
                membership.getRole().name(),
                membership.getStatus().name(),
                membership.getJoinedAt()
        );
    }
}
