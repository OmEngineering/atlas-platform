package com.atlas.modules.billing.dto;

import java.time.Instant;
import java.util.UUID;

public record SubscriptionResponse(
        UUID id,
        UUID organizationId,
        String plan,
        int seats,
        int usedSeats,
        String status,
        Instant currentPeriodEnd,
        boolean stripeEnabled
) {
}
