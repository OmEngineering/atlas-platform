package com.atlas.modules.billing.service;

import com.atlas.modules.billing.dto.CheckoutSessionResponse;
import com.atlas.modules.billing.entity.Subscription;
import com.atlas.modules.billing.entity.SubscriptionPlan;

import java.util.UUID;

public interface StripeCheckoutPort {

    CheckoutSessionResponse createCheckoutSession(
            Subscription subscription,
            SubscriptionPlan targetPlan,
            int seats,
            UUID actorId);
}
