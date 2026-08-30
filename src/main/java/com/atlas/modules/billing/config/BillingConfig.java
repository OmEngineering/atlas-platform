package com.atlas.modules.billing.config;

import com.atlas.config.StripeProperties;
import com.atlas.modules.billing.service.StubStripeCheckoutAdapter;
import com.atlas.modules.billing.service.StripeCheckoutAdapter;
import com.atlas.modules.billing.service.StripeCheckoutPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BillingConfig {

    @Bean
    StripeCheckoutPort stripeCheckoutPort(
            StripeProperties stripeProperties,
            StripeCheckoutAdapter stripeCheckoutAdapter,
            StubStripeCheckoutAdapter stubStripeCheckoutAdapter) {
        if (stripeProperties.isEnabled()) {
            return stripeCheckoutAdapter;
        }
        return stubStripeCheckoutAdapter;
    }
}
