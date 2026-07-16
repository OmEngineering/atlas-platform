package com.atlas.modules.billing.service;

import com.atlas.exception.ForbiddenException;
import com.atlas.modules.organizations.entity.Membership;
import com.atlas.modules.organizations.entity.MembershipRole;
import com.atlas.modules.organizations.entity.MembershipStatus;
import com.atlas.modules.organizations.repository.MembershipRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class BillingAuthorizationService {

    private final MembershipRepository membershipRepository;

    public BillingAuthorizationService(MembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    public Membership requireBillingAccess(UUID organizationId, UUID userId) {
        Membership membership = membershipRepository
                .findByOrganizationIdAndUserIdAndStatus(organizationId, userId, MembershipStatus.ACTIVE)
                .orElseThrow(() -> new ForbiddenException("You do not have access to this organization"));

        if (membership.getRole() != MembershipRole.OWNER
                && membership.getRole() != MembershipRole.BILLING_MANAGER) {
            throw new ForbiddenException("Insufficient permissions for billing");
        }
        return membership;
    }
}
