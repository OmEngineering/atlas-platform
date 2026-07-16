package com.atlas.modules.billing.mapper;

import com.atlas.modules.billing.dto.SubscriptionResponse;
import com.atlas.modules.billing.entity.Subscription;

public final class SubscriptionMapper {

    private SubscriptionMapper() {
    }

    public static SubscriptionResponse toResponse(Subscription subscription, int usedSeats, boolean stripeEnabled) {
        return new SubscriptionResponse(
                subscription.getId(),
                subscription.getOrganizationId(),
                subscription.getPlan().name(),
                subscription.getSeats(),
                usedSeats,
                subscription.getStatus().name(),
                subscription.getCurrentPeriodEnd(),
                stripeEnabled
        );
    }
}
