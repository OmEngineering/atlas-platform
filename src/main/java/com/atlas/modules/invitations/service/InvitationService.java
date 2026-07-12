package com.atlas.modules.invitations.service;

import com.atlas.exception.ApiException;
import com.atlas.exception.ErrorCode;
import com.atlas.exception.ForbiddenException;
import com.atlas.exception.GoneException;
import com.atlas.exception.ResourceNotFoundException;
import com.atlas.modules.auth.entity.User;
import com.atlas.modules.auth.repository.UserRepository;
import com.atlas.modules.auth.service.TokenHasher;
import com.atlas.modules.billing.entity.Subscription;
import com.atlas.modules.billing.repository.SubscriptionRepository;
import com.atlas.modules.invitations.dto.CreateInvitationRequest;
import com.atlas.modules.invitations.dto.InvitationResponse;
import com.atlas.modules.invitations.entity.Invitation;
import com.atlas.modules.invitations.entity.InvitationStatus;
import com.atlas.modules.invitations.repository.InvitationRepository;
import com.atlas.modules.organizations.entity.Membership;
import com.atlas.modules.organizations.entity.MembershipRole;
import com.atlas.modules.organizations.entity.MembershipStatus;
import com.atlas.modules.organizations.repository.MembershipRepository;
import com.atlas.modules.organizations.service.MembershipAuthorizationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class InvitationService {

    private final InvitationRepository invitationRepository;
    private final MembershipRepository membershipRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final MembershipAuthorizationService membershipAuthorizationService;
    private final UserRepository userRepository;
    private final TokenHasher tokenHasher;

    public InvitationService(
            InvitationRepository invitationRepository,
            MembershipRepository membershipRepository,
            SubscriptionRepository subscriptionRepository,
            MembershipAuthorizationService membershipAuthorizationService,
            UserRepository userRepository,
            TokenHasher tokenHasher) {
        this.invitationRepository = invitationRepository;
        this.membershipRepository = membershipRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.membershipAuthorizationService = membershipAuthorizationService;
        this.userRepository = userRepository;
        this.tokenHasher = tokenHasher;
    }

    @Transactional
    public InvitationResponse create(UUID organizationId, UUID inviterId, CreateInvitationRequest request) {
        membershipAuthorizationService.requireActiveMembership(organizationId, inviterId, MembershipRole.ADMIN);

        String email = normalizeEmail(request.email());
        MembershipRole role = parseOrgRole(request.role());

        userRepository.findByEmailIgnoreCase(email).ifPresent(existingUser ->
                membershipRepository.findByOrganizationIdAndUserIdAndStatus(
                        organizationId, existingUser.getId(), MembershipStatus.ACTIVE)
                        .ifPresent(ignored -> {
                            throw new ApiException(ErrorCode.ALREADY_MEMBER, "User is already a member", HttpStatus.CONFLICT);
                        }));

        enforceSeatLimit(organizationId);

        invitationRepository.findByOrganizationIdAndEmailAndStatus(organizationId, email, InvitationStatus.PENDING)
                .ifPresent(existing -> {
                    existing.setStatus(InvitationStatus.REVOKED);
                    invitationRepository.save(existing);
                });

        String rawToken = tokenHasher.generateRefreshToken();
        Invitation invitation = new Invitation();
        invitation.setOrganizationId(organizationId);
        invitation.setEmail(email);
        invitation.setRole(role.name());
        invitation.setToken(tokenHasher.hash(rawToken));
        invitation.setInvitedBy(inviterId);
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        invitation = invitationRepository.save(invitation);

        return toResponse(invitation, rawToken);
    }

    @Transactional(readOnly = true)
    public List<InvitationResponse> listPending(UUID organizationId, UUID userId) {
        membershipAuthorizationService.requireActiveMembership(organizationId, userId, MembershipRole.ADMIN);
        return invitationRepository.findByOrganizationIdAndStatus(organizationId, InvitationStatus.PENDING).stream()
                .map(invitation -> toResponse(invitation, null))
                .toList();
    }

    @Transactional
    public void revoke(UUID invitationId, UUID userId) {
        Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));
        membershipAuthorizationService.requireActiveMembership(invitation.getOrganizationId(), userId, MembershipRole.ADMIN);

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new ApiException(ErrorCode.CONFLICT, "Invitation is no longer pending", HttpStatus.CONFLICT);
        }
        invitation.setStatus(InvitationStatus.REVOKED);
        invitationRepository.save(invitation);
    }

    @Transactional
    public Membership accept(UUID invitationId, User user) {
        Invitation invitation = invitationRepository.findByIdAndStatus(invitationId, InvitationStatus.PENDING)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));

        if (!normalizeEmail(user.getEmail()).equals(invitation.getEmail())) {
            throw new ForbiddenException("This invitation was sent to a different email address");
        }
        if (invitation.getExpiresAt().isBefore(Instant.now())) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            invitationRepository.save(invitation);
            throw new GoneException(ErrorCode.INVITATION_EXPIRED, "Invitation has expired");
        }
        if (invitation.getProjectId() != null) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Project invites are not supported yet", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        membershipRepository.findByOrganizationIdAndUserIdAndStatus(
                invitation.getOrganizationId(), user.getId(), MembershipStatus.ACTIVE)
                .ifPresent(ignored -> {
                    throw new ApiException(ErrorCode.ALREADY_MEMBER, "Already a member of this organization", HttpStatus.CONFLICT);
                });

        enforceSeatLimit(invitation.getOrganizationId());

        Membership membership = new Membership();
        membership.setOrganizationId(invitation.getOrganizationId());
        membership.setUserId(user.getId());
        membership.setRole(MembershipRole.valueOf(invitation.getRole()));
        membership.setInvitedBy(invitation.getInvitedBy());
        membership.setStatus(MembershipStatus.ACTIVE);
        membership.setJoinedAt(Instant.now());
        membershipRepository.save(membership);

        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitationRepository.save(invitation);
        return membership;
    }

    private void enforceSeatLimit(UUID organizationId) {
        Subscription subscription = subscriptionRepository.findByOrganizationId(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));
        long activeMembers = membershipRepository.countByOrganizationIdAndStatus(organizationId, MembershipStatus.ACTIVE);
        if (activeMembers >= subscription.getSeats()) {
            throw new ApiException(ErrorCode.SEAT_LIMIT_EXCEEDED, "No seats available on current plan", HttpStatus.FORBIDDEN);
        }
    }

    private MembershipRole parseOrgRole(String role) {
        try {
            MembershipRole parsed = MembershipRole.valueOf(role.trim().toUpperCase(Locale.ROOT));
            if (parsed == MembershipRole.OWNER) {
                throw new ApiException(ErrorCode.VALIDATION_ERROR, "Cannot invite as owner", HttpStatus.UNPROCESSABLE_ENTITY);
            }
            return parsed;
        } catch (IllegalArgumentException ex) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Invalid role for organization invite", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private InvitationResponse toResponse(Invitation invitation, String rawToken) {
        return new InvitationResponse(
                invitation.getId(),
                invitation.getOrganizationId(),
                invitation.getEmail(),
                invitation.getRole(),
                invitation.getStatus().name(),
                invitation.getExpiresAt(),
                invitation.getCreatedAt(),
                rawToken
        );
    }
}
