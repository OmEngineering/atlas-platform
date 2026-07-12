package com.atlas.modules.organizations.service;

import com.atlas.exception.ApiException;
import com.atlas.exception.ErrorCode;
import com.atlas.exception.ResourceNotFoundException;
import com.atlas.modules.auth.entity.User;
import com.atlas.modules.billing.entity.Subscription;
import com.atlas.modules.billing.entity.SubscriptionPlan;
import com.atlas.modules.billing.entity.SubscriptionStatus;
import com.atlas.modules.billing.repository.SubscriptionRepository;
import com.atlas.modules.billing.service.SubscriptionService;
import com.atlas.modules.organizations.dto.CreateOrganizationRequest;
import com.atlas.modules.organizations.dto.MembershipResponse;
import com.atlas.modules.organizations.dto.OrganizationResponse;
import com.atlas.modules.organizations.dto.UpdateOrganizationRequest;
import com.atlas.modules.organizations.entity.Membership;
import com.atlas.modules.organizations.entity.MembershipRole;
import com.atlas.modules.organizations.entity.MembershipStatus;
import com.atlas.modules.organizations.entity.Organization;
import com.atlas.modules.organizations.entity.OrganizationStatus;
import com.atlas.modules.organizations.mapper.OrganizationMapper;
import com.atlas.modules.organizations.repository.MembershipRepository;
import com.atlas.modules.organizations.repository.OrganizationRepository;
import com.atlas.shared.dto.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final MembershipRepository membershipRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionService subscriptionService;
    private final MembershipAuthorizationService membershipAuthorizationService;

    public OrganizationService(
            OrganizationRepository organizationRepository,
            MembershipRepository membershipRepository,
            SubscriptionRepository subscriptionRepository,
            SubscriptionService subscriptionService,
            MembershipAuthorizationService membershipAuthorizationService) {
        this.organizationRepository = organizationRepository;
        this.membershipRepository = membershipRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionService = subscriptionService;
        this.membershipAuthorizationService = membershipAuthorizationService;
    }

    @Transactional
    public OrganizationResponse createOrganization(CreateOrganizationRequest request, User creator) {
        String slug = request.slug().trim().toLowerCase(Locale.ROOT);
        if (organizationRepository.existsBySlugIgnoreCase(slug)) {
            throw new ApiException(ErrorCode.SLUG_TAKEN, "Organization slug is already taken", HttpStatus.CONFLICT);
        }

        Organization organization = new Organization();
        organization.setName(request.name().trim());
        organization.setSlug(slug);
        organization.setTimezone(request.timezone().trim());
        organization.setCountry(request.country());
        organization.setStatus(OrganizationStatus.ACTIVE);
        organization.setCreatedBy(creator.getId());
        organization = organizationRepository.save(organization);

        Subscription subscription = subscriptionService.createFreeSubscription(organization.getId());
        organization.setSubscriptionId(subscription.getId());
        organization = organizationRepository.save(organization);

        Membership membership = new Membership();
        membership.setOrganizationId(organization.getId());
        membership.setUserId(creator.getId());
        membership.setRole(MembershipRole.OWNER);
        membership.setStatus(MembershipStatus.ACTIVE);
        membership.setJoinedAt(Instant.now());
        membershipRepository.save(membership);

        return OrganizationMapper.toResponse(organization);
    }

    @Transactional(readOnly = true)
    public OrganizationResponse getOrganization(UUID organizationId, UUID userId) {
        membershipAuthorizationService.requireAnyActiveMembership(userId, organizationId);
        Organization organization = findActiveOrganization(organizationId);
        return OrganizationMapper.toResponse(organization);
    }

    @Transactional
    public OrganizationResponse updateOrganization(UUID organizationId, UUID userId, UpdateOrganizationRequest request) {
        membershipAuthorizationService.requireActiveMembership(userId, organizationId, MembershipRole.ADMIN);
        Organization organization = findActiveOrganization(organizationId);

        if (request.name() != null) {
            organization.setName(request.name().trim());
        }
        if (request.logoUrl() != null) {
            organization.setLogoUrl(request.logoUrl());
        }
        if (request.description() != null) {
            organization.setDescription(request.description());
        }
        if (request.timezone() != null) {
            organization.setTimezone(request.timezone().trim());
        }
        if (request.country() != null) {
            organization.setCountry(request.country());
        }

        return OrganizationMapper.toResponse(organizationRepository.save(organization));
    }

    @Transactional
    public void archiveOrganization(UUID organizationId, UUID userId) {
        membershipAuthorizationService.requireActiveMembership(userId, organizationId, MembershipRole.OWNER);
        Organization organization = findActiveOrganization(organizationId);

        Subscription subscription = subscriptionRepository.findByOrganizationId(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));

        if (subscription.getPlan() != SubscriptionPlan.FREE && subscription.getStatus() == SubscriptionStatus.ACTIVE) {
            throw new ApiException(
                    ErrorCode.CONFLICT,
                    "Cancel the active paid subscription before archiving the organization",
                    HttpStatus.CONFLICT);
        }

        organization.setStatus(OrganizationStatus.ARCHIVED);
        organization.setDeletedAt(Instant.now());
        organizationRepository.save(organization);
    }

    @Transactional(readOnly = true)
    public PageResponse<MembershipResponse> listMembers(UUID organizationId, UUID userId, int page, int pageSize) {
        membershipAuthorizationService.requireAnyActiveMembership(userId, organizationId);
        Page<Membership> memberships = membershipRepository.findByOrganizationIdAndStatus(
                organizationId,
                MembershipStatus.ACTIVE,
                PageRequest.of(page, pageSize));

        List<MembershipResponse> items = memberships.getContent().stream()
                .map(OrganizationMapper::toMembershipResponse)
                .toList();

        return new PageResponse<>(
                items,
                memberships.getNumber(),
                memberships.getSize(),
                memberships.getTotalElements(),
                memberships.getTotalPages());
    }

    private Organization findActiveOrganization(UUID organizationId) {
        return organizationRepository.findByIdAndDeletedAtIsNull(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
    }
}
