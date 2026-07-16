package com.atlas.modules.memberships.service;

import com.atlas.exception.ApiException;
import com.atlas.exception.ErrorCode;
import com.atlas.exception.ResourceNotFoundException;
import com.atlas.modules.audit.entity.AuditActorType;
import com.atlas.modules.audit.entity.AuditTargetType;
import com.atlas.modules.audit.service.AuditLogService;
import com.atlas.modules.organizations.dto.MembershipResponse;
import com.atlas.modules.organizations.entity.Membership;
import com.atlas.modules.organizations.entity.MembershipRole;
import com.atlas.modules.organizations.entity.MembershipStatus;
import com.atlas.modules.organizations.mapper.OrganizationMapper;
import com.atlas.modules.organizations.repository.MembershipRepository;
import com.atlas.modules.organizations.service.MembershipAuthorizationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class MembershipManagementService {

    private final MembershipRepository membershipRepository;
    private final MembershipAuthorizationService membershipAuthorizationService;
    private final AuditLogService auditLogService;

    public MembershipManagementService(
            MembershipRepository membershipRepository,
            MembershipAuthorizationService membershipAuthorizationService,
            AuditLogService auditLogService) {
        this.membershipRepository = membershipRepository;
        this.membershipAuthorizationService = membershipAuthorizationService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public MembershipResponse updateRole(UUID membershipId, UUID actorId, String role) {
        Membership membership = findActiveMembership(membershipId);
        membershipAuthorizationService.requireActiveMembership(
                membership.getOrganizationId(), actorId, MembershipRole.ADMIN);

        MembershipRole newRole = MembershipRole.valueOf(role.trim().toUpperCase(Locale.ROOT));
        if (newRole == MembershipRole.OWNER) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Use transfer ownership to assign owner", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (membership.getRole() == MembershipRole.OWNER) {
            throw new ApiException(ErrorCode.CONFLICT, "Cannot change owner role here", HttpStatus.CONFLICT);
        }

        MembershipRole previousRole = membership.getRole();
        membership.setRole(newRole);
        membership = membershipRepository.save(membership);

        auditLogService.record(
                membership.getOrganizationId(),
                actorId,
                AuditActorType.USER,
                "membership.role_changed",
                AuditTargetType.MEMBERSHIP,
                membership.getId(),
                Map.of("userId", membership.getUserId().toString(), "from", previousRole.name(), "to", newRole.name()));

        return OrganizationMapper.toMembershipResponse(membership);
    }

    @Transactional
    public void removeMember(UUID membershipId, UUID actorId) {
        Membership membership = findActiveMembership(membershipId);
        membershipAuthorizationService.requireActiveMembership(
                membership.getOrganizationId(), actorId, MembershipRole.ADMIN);

        if (membership.getRole() == MembershipRole.OWNER) {
            throw new ApiException(ErrorCode.CONFLICT, "Owner cannot be removed", HttpStatus.CONFLICT);
        }

        membership.setStatus(MembershipStatus.REMOVED);
        membershipRepository.save(membership);

        auditLogService.record(
                membership.getOrganizationId(),
                actorId,
                AuditActorType.USER,
                "membership.removed",
                AuditTargetType.MEMBERSHIP,
                membership.getId(),
                Map.of("userId", membership.getUserId().toString(), "role", membership.getRole().name()));
    }

    private Membership findActiveMembership(UUID membershipId) {
        Membership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership not found"));
        if (membership.getStatus() != MembershipStatus.ACTIVE) {
            throw new ResourceNotFoundException("Membership not found");
        }
        return membership;
    }
}
