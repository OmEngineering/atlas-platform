package com.atlas.modules.billing.service;

import com.atlas.config.StripeProperties;
import com.atlas.exception.ApiException;
import com.atlas.modules.audit.service.AuditLogService;
import com.atlas.modules.billing.dto.ChangePlanRequest;
import com.atlas.modules.billing.entity.Subscription;
import com.atlas.modules.billing.entity.SubscriptionPlan;
import com.atlas.modules.billing.entity.SubscriptionStatus;
import com.atlas.modules.billing.repository.SubscriptionRepository;
import com.atlas.modules.organizations.entity.Membership;
import com.atlas.modules.organizations.entity.MembershipRole;
import com.atlas.modules.organizations.entity.MembershipStatus;
import com.atlas.modules.organizations.repository.MembershipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private MembershipRepository membershipRepository;
    @Mock
    private BillingAuthorizationService billingAuthorizationService;
    @Mock
    private StripeCheckoutPort stripeCheckoutPort;
    @Mock
    private AuditLogService auditLogService;

    private BillingService billingService;

    private final UUID organizationId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        StripeProperties stripeProperties = new StripeProperties(null, null, null, null, null, null);
        billingService = new BillingService(
                subscriptionRepository,
                membershipRepository,
                billingAuthorizationService,
                stripeCheckoutPort,
                stripeProperties,
                auditLogService);
    }

    @Test
    void changePlanDowngradesToFree() {
        Subscription subscription = activeSubscription(SubscriptionPlan.TEAM, 5);
        when(billingAuthorizationService.requireBillingAccess(organizationId, userId))
                .thenReturn(ownerMembership());
        when(subscriptionRepository.findByOrganizationId(organizationId))
                .thenReturn(Optional.of(subscription));
        when(membershipRepository.countByOrganizationIdAndStatus(organizationId, MembershipStatus.ACTIVE))
                .thenReturn(2L);
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = billingService.changePlan(organizationId, userId, new ChangePlanRequest("FREE", 2));

        assertThat(response.plan()).isEqualTo("FREE");
        assertThat(response.seats()).isEqualTo(2);
        assertThat(subscription.getProviderSubscriptionId()).isNull();
        verify(auditLogService).record(eq(organizationId), eq(userId), any(), eq("subscription.changed"), any(), any(), any());
    }

    @Test
    void changePlanRejectsSeatsBelowActiveMembers() {
        Subscription subscription = activeSubscription(SubscriptionPlan.FREE, 5);
        when(billingAuthorizationService.requireBillingAccess(organizationId, userId))
                .thenReturn(ownerMembership());
        when(subscriptionRepository.findByOrganizationId(organizationId))
                .thenReturn(Optional.of(subscription));
        when(membershipRepository.countByOrganizationIdAndStatus(organizationId, MembershipStatus.ACTIVE))
                .thenReturn(3L);

        assertThatThrownBy(() -> billingService.changePlan(organizationId, userId, new ChangePlanRequest("FREE", 2)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("active members");
    }

    private Subscription activeSubscription(SubscriptionPlan plan, int seats) {
        Subscription subscription = new Subscription();
        subscription.setId(UUID.randomUUID());
        subscription.setOrganizationId(organizationId);
        subscription.setPlan(plan);
        subscription.setSeats(seats);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setProviderSubscriptionId("sub_test");
        return subscription;
    }

    private Membership ownerMembership() {
        Membership membership = new Membership();
        membership.setRole(MembershipRole.OWNER);
        membership.setStatus(MembershipStatus.ACTIVE);
        return membership;
    }
}
