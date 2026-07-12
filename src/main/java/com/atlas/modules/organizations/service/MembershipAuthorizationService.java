package com.atlas.modules.organizations.service;

import com.atlas.exception.ForbiddenException;
import com.atlas.exception.ResourceNotFoundException;
import com.atlas.modules.organizations.entity.Membership;
import com.atlas.modules.organizations.entity.MembershipRole;
import com.atlas.modules.organizations.entity.MembershipStatus;
import com.atlas.modules.organizations.repository.MembershipRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class MembershipAuthorizationService {

    private final MembershipRepository membershipRepository;

    public MembershipAuthorizationService(MembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    public Membership requireActiveMembership(UUID userId, UUID organizationId, MembershipRole minimumRole) {
        Membership membership = membershipRepository
                .findByOrganizationIdAndUserIdAndStatus(organizationId, userId, MembershipStatus.ACTIVE)
                .orElseThrow(() -> new ForbiddenException("You do not have access to this organization"));

        if (!membership.getRole().isAtLeast(minimumRole)) {
            throw new ForbiddenException("Insufficient permissions for this operation");
        }
        return membership;
    }

    public Membership requireAnyActiveMembership(UUID userId, UUID organizationId) {
        return requireActiveMembership(userId, organizationId, MembershipRole.MEMBER);
    }
}
