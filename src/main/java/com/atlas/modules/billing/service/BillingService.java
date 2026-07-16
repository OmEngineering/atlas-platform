package com.atlas.modules.billing.service;

import com.atlas.exception.ApiException;
import com.atlas.exception.ErrorCode;
import com.atlas.exception.ResourceNotFoundException;
import com.atlas.modules.audit.entity.AuditActorType;
import com.atlas.modules.audit.entity.AuditTargetType;
import com.atlas.modules.audit.service.AuditLogService;
import com.atlas.modules.billing.dto.ChangePlanRequest;
import com.atlas.modules.billing.dto.CheckoutSessionResponse;
import com.atlas.modules.billing.dto.CreateCheckoutRequest;
import com.atlas.modules.billing.dto.SubscriptionResponse;
import com.atlas.modules.billing.entity.Subscription;
import com.atlas.modules.billing.entity.SubscriptionPlan;
import com.atlas.modules.billing.entity.SubscriptionStatus;
import com.atlas.modules.billing.mapper.SubscriptionMapper;
import com.atlas.modules.billing.repository.SubscriptionRepository;
import com.atlas.config.StripeProperties;
import com.atlas.modules.organizations.entity.MembershipStatus;
import com.atlas.modules.organizations.repository.MembershipRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class BillingService {

    private final SubscriptionRepository subscriptionRepository;
    private final MembershipRepository membershipRepository;
    private final BillingAuthorizationService billingAuthorizationService;
    private final StripeCheckoutPort stripeCheckoutPort;
    private final StripeProperties stripeProperties;
    private final AuditLogService auditLogService;

    public BillingService(
            SubscriptionRepository subscriptionRepository,
            MembershipRepository membershipRepository,
            BillingAuthorizationService billingAuthorizationService,
            StripeCheckoutPort stripeCheckoutPort,
            StripeProperties stripeProperties,
            AuditLogService auditLogService) {
        this.subscriptionRepository = subscriptionRepository;
        this.membershipRepository = membershipRepository;
        this.billingAuthorizationService = billingAuthorizationService;
        this.stripeCheckoutPort = stripeCheckoutPort;
        this.stripeProperties = stripeProperties;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public SubscriptionResponse getSubscription(UUID organizationId, UUID userId) {
        billingAuthorizationService.requireBillingAccess(organizationId, userId);
        Subscription subscription = findSubscription(organizationId);
        int usedSeats = countActiveMembers(organizationId);
        return SubscriptionMapper.toResponse(subscription, usedSeats, stripeProperties.isEnabled());
    }

    @Transactional
    public CheckoutSessionResponse createCheckout(
            UUID organizationId, UUID userId, CreateCheckoutRequest request) {
        billingAuthorizationService.requireBillingAccess(organizationId, userId);
        Subscription subscription = findSubscription(organizationId);
        SubscriptionPlan targetPlan = parsePaidPlan(request.plan());
        int seats = request.seats();
        validateSeats(organizationId, seats);

        return stripeCheckoutPort.createCheckoutSession(subscription, targetPlan, seats, userId);
    }

    @Transactional
    public SubscriptionResponse changePlan(UUID organizationId, UUID userId, ChangePlanRequest request) {
        billingAuthorizationService.requireBillingAccess(organizationId, userId);
        Subscription subscription = findSubscription(organizationId);
        SubscriptionPlan newPlan = parsePlan(request.plan());
        int seats = request.seats();
        validateSeats(organizationId, seats);

        if (newPlan != SubscriptionPlan.FREE && subscription.getProviderSubscriptionId() == null) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    "Use checkout to start a paid plan",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        SubscriptionPlan previousPlan = subscription.getPlan();
        int previousSeats = subscription.getSeats();
        subscription.setPlan(newPlan);
        subscription.setSeats(seats);
        if (newPlan == SubscriptionPlan.FREE) {
            subscription.setStatus(SubscriptionStatus.ACTIVE);
            subscription.setProviderSubscriptionId(null);
            subscription.setCurrentPeriodEnd(null);
        }
        subscription = subscriptionRepository.save(subscription);

        auditLogService.record(
                organizationId,
                userId,
                AuditActorType.USER,
                "subscription.changed",
                AuditTargetType.SUBSCRIPTION,
                subscription.getId(),
                Map.of(
                        "fromPlan", previousPlan.name(),
                        "toPlan", newPlan.name(),
                        "fromSeats", String.valueOf(previousSeats),
                        "toSeats", String.valueOf(seats)));

        return SubscriptionMapper.toResponse(subscription, countActiveMembers(organizationId), stripeProperties.isEnabled());
    }

    @Transactional
    public void applyCheckoutSuccess(UUID organizationId, SubscriptionPlan plan, int seats, String providerSubscriptionId) {
        Subscription subscription = findSubscription(organizationId);
        subscription.setPlan(plan);
        subscription.setSeats(seats);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setProviderSubscriptionId(providerSubscriptionId);
        subscriptionRepository.save(subscription);

        auditLogService.record(
                organizationId,
                null,
                AuditActorType.SYSTEM,
                "subscription.updated",
                AuditTargetType.SUBSCRIPTION,
                subscription.getId(),
                Map.of("plan", plan.name(), "seats", String.valueOf(seats), "source", "stripe_checkout"));
    }

    @Transactional
    public void markPastDue(UUID organizationId) {
        Subscription subscription = findSubscription(organizationId);
        subscription.setStatus(SubscriptionStatus.PAST_DUE);
        subscriptionRepository.save(subscription);

        auditLogService.record(
                organizationId,
                null,
                AuditActorType.SYSTEM,
                "subscription.payment_failed",
                AuditTargetType.SUBSCRIPTION,
                subscription.getId(),
                Map.of());
    }

    private Subscription findSubscription(UUID organizationId) {
        return subscriptionRepository.findByOrganizationId(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));
    }

    private int countActiveMembers(UUID organizationId) {
        return (int) membershipRepository.countByOrganizationIdAndStatus(organizationId, MembershipStatus.ACTIVE);
    }

    private void validateSeats(UUID organizationId, int seats) {
        int used = countActiveMembers(organizationId);
        if (seats < used) {
            throw new ApiException(
                    ErrorCode.SEAT_LIMIT_EXCEEDED,
                    "Seats cannot be less than active members (" + used + ")",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private SubscriptionPlan parsePaidPlan(String plan) {
        SubscriptionPlan parsed = parsePlan(plan);
        if (parsed == SubscriptionPlan.FREE || parsed == SubscriptionPlan.ENTERPRISE) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    "Checkout is only for TEAM or BUSINESS plans",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return parsed;
    }

    private SubscriptionPlan parsePlan(String plan) {
        try {
            return SubscriptionPlan.valueOf(plan.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Invalid plan", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }
}
