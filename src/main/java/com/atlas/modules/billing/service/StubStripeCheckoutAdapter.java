package com.atlas.modules.billing.service;

import com.atlas.modules.billing.dto.CheckoutSessionResponse;
import com.atlas.modules.billing.entity.Subscription;
import com.atlas.modules.billing.entity.SubscriptionPlan;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class StubStripeCheckoutAdapter implements StripeCheckoutPort {

    @Override
    public CheckoutSessionResponse createCheckoutSession(
            Subscription subscription,
            SubscriptionPlan targetPlan,
            int seats,
            UUID actorId) {
        String sessionId = "stub_cs_" + UUID.randomUUID();
        String url = "https://checkout.stripe.test/" + sessionId
                + "?org=" + subscription.getOrganizationId()
                + "&plan=" + targetPlan.name()
                + "&seats=" + seats;
        return new CheckoutSessionResponse(url, sessionId);
    }
}
