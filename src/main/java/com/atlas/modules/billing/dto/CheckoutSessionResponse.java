package com.atlas.modules.billing.dto;

public record CheckoutSessionResponse(
        String checkoutUrl,
        String sessionId
) {
}
