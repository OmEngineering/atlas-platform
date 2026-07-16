package com.atlas.modules.billing.service;

import com.atlas.config.StripeProperties;
import com.atlas.modules.billing.dto.CheckoutSessionResponse;
import com.atlas.modules.billing.entity.Subscription;
import com.atlas.modules.billing.entity.SubscriptionPlan;
import com.atlas.modules.billing.repository.SubscriptionRepository;
import com.stripe.Stripe;
import com.stripe.model.Customer;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class StripeCheckoutAdapter implements StripeCheckoutPort {

    private final StripeProperties stripeProperties;
    private final SubscriptionRepository subscriptionRepository;

    public StripeCheckoutAdapter(StripeProperties stripeProperties, SubscriptionRepository subscriptionRepository) {
        this.stripeProperties = stripeProperties;
        this.subscriptionRepository = subscriptionRepository;
    }

    @Override
    public CheckoutSessionResponse createCheckoutSession(
            Subscription subscription,
            SubscriptionPlan targetPlan,
            int seats,
            UUID actorId) {
        Stripe.apiKey = stripeProperties.apiKey();

        String customerId = subscription.getProviderCustomerId();
        if (customerId == null || customerId.isBlank()) {
            try {
                Customer customer = createCustomer(subscription.getOrganizationId());
                customerId = customer.getId();
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to create Stripe customer", ex);
            }
            subscription.setProviderCustomerId(customerId);
            subscriptionRepository.save(subscription);
        }

        String priceId = priceIdForPlan(targetPlan);
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setCustomer(customerId)
                .setSuccessUrl(stripeProperties.successUrl())
                .setCancelUrl(stripeProperties.cancelUrl())
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setPrice(priceId)
                        .setQuantity((long) seats)
                        .build())
                .putMetadata("organizationId", subscription.getOrganizationId().toString())
                .putMetadata("plan", targetPlan.name())
                .putMetadata("seats", String.valueOf(seats))
                .putMetadata("actorId", actorId.toString())
                .build();

        try {
            Session session = Session.create(params);
            return new CheckoutSessionResponse(session.getUrl(), session.getId());
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to create Stripe checkout session", ex);
        }
    }

    private Customer createCustomer(UUID organizationId) throws Exception {
        CustomerCreateParams params = CustomerCreateParams.builder()
                .putMetadata("organizationId", organizationId.toString())
                .build();
        return Customer.create(params);
    }

    private String priceIdForPlan(SubscriptionPlan plan) {
        return switch (plan) {
            case TEAM -> requirePrice(stripeProperties.teamPriceId(), "TEAM");
            case BUSINESS -> requirePrice(stripeProperties.businessPriceId(), "BUSINESS");
            default -> throw new IllegalArgumentException("Plan requires checkout: " + plan);
        };
    }

    private String requirePrice(String priceId, String label) {
        if (priceId == null || priceId.isBlank()) {
            throw new IllegalStateException("Stripe price id not configured for " + label);
        }
        return priceId;
    }
}
