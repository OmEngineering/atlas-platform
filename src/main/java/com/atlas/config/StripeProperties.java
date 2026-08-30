package com.atlas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "atlas.billing.stripe")
public record StripeProperties(
        String apiKey,
        String webhookSecret,
        String successUrl,
        String cancelUrl,
        String teamPriceId,
        String businessPriceId
) {
    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }
}
