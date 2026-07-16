package com.atlas.modules.billing.service;

import com.atlas.config.StripeProperties;
import com.atlas.modules.billing.entity.SubscriptionPlan;
import com.atlas.modules.billing.repository.SubscriptionRepository;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Invoice;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class StripeWebhookService {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookService.class);

    private final StripeProperties stripeProperties;
    private final BillingService billingService;
    private final SubscriptionRepository subscriptionRepository;

    public StripeWebhookService(
            StripeProperties stripeProperties,
            BillingService billingService,
            SubscriptionRepository subscriptionRepository) {
        this.stripeProperties = stripeProperties;
        this.billingService = billingService;
        this.subscriptionRepository = subscriptionRepository;
    }

    public void handle(String payload, String signatureHeader) {
        if (!stripeProperties.isEnabled()) {
            throw new IllegalStateException("Stripe is not configured");
        }
        if (stripeProperties.webhookSecret() == null || stripeProperties.webhookSecret().isBlank()) {
            throw new IllegalStateException("Stripe webhook secret is not configured");
        }

        Event event;
        try {
            event = Webhook.constructEvent(payload, signatureHeader, stripeProperties.webhookSecret());
        } catch (SignatureVerificationException ex) {
            throw new IllegalArgumentException("Invalid Stripe signature", ex);
        }

        switch (event.getType()) {
            case "checkout.session.completed" -> handleCheckoutCompleted(event);
            case "invoice.payment_failed" -> handlePaymentFailed(event);
            default -> log.debug("Ignoring Stripe event type {}", event.getType());
        }
    }

    private void handleCheckoutCompleted(Event event) {
        Session session = (Session) event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow(() -> new IllegalStateException("Missing checkout session payload"));

        Map<String, String> metadata = session.getMetadata();
        UUID organizationId = UUID.fromString(metadata.get("organizationId"));
        SubscriptionPlan plan = SubscriptionPlan.valueOf(metadata.get("plan"));
        int seats = Integer.parseInt(metadata.get("seats"));
        String providerSubscriptionId = session.getSubscription();

        billingService.applyCheckoutSuccess(organizationId, plan, seats, providerSubscriptionId);
    }

    private void handlePaymentFailed(Event event) {
        Invoice invoice = (Invoice) event.getDataObjectDeserializer()
                .getObject()
                .orElse(null);
        if (invoice == null || invoice.getCustomer() == null) {
            return;
        }
        subscriptionRepository.findByProviderCustomerId(invoice.getCustomer())
                .ifPresent(subscription -> billingService.markPastDue(subscription.getOrganizationId()));
    }
}
