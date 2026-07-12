package com.atlas.modules.billing.service;

import com.atlas.modules.billing.entity.Subscription;
import com.atlas.modules.billing.entity.SubscriptionPlan;
import com.atlas.modules.billing.entity.SubscriptionStatus;
import com.atlas.modules.billing.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;

    public SubscriptionService(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    @Transactional
    public Subscription createFreeSubscription(UUID organizationId) {
        Subscription subscription = new Subscription();
        subscription.setOrganizationId(organizationId);
        subscription.setPlan(SubscriptionPlan.FREE);
        subscription.setSeats(1);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        return subscriptionRepository.save(subscription);
    }
}
