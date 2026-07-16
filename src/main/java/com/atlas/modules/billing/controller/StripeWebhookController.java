package com.atlas.modules.billing.controller;

import com.atlas.modules.billing.service.StripeWebhookService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/webhooks/stripe")
public class StripeWebhookController {

    private final StripeWebhookService stripeWebhookService;

    public StripeWebhookController(StripeWebhookService stripeWebhookService) {
        this.stripeWebhookService = stripeWebhookService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void handle(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {
        stripeWebhookService.handle(payload, signature);
    }
}
