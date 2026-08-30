package com.atlas.modules.billing.controller;

import com.atlas.modules.billing.dto.ChangePlanRequest;
import com.atlas.modules.billing.dto.CheckoutSessionResponse;
import com.atlas.modules.billing.dto.CreateCheckoutRequest;
import com.atlas.modules.billing.dto.SubscriptionResponse;
import com.atlas.modules.billing.service.BillingService;
import com.atlas.shared.dto.ApiResponse;
import com.atlas.shared.security.SecurityContextAccessor;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/subscription")
@Tag(name = "Billing")
public class OrganizationSubscriptionController {

    private final BillingService billingService;
    private final SecurityContextAccessor securityContextAccessor;

    public OrganizationSubscriptionController(
            BillingService billingService,
            SecurityContextAccessor securityContextAccessor) {
        this.billingService = billingService;
        this.securityContextAccessor = securityContextAccessor;
    }

    @GetMapping
    public ApiResponse<SubscriptionResponse> get(@PathVariable UUID organizationId) {
        return ApiResponse.of(billingService.getSubscription(
                organizationId, securityContextAccessor.currentUserId()));
    }

    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CheckoutSessionResponse> checkout(
            @PathVariable UUID organizationId,
            @Valid @RequestBody CreateCheckoutRequest request) {
        return ApiResponse.of(billingService.createCheckout(
                organizationId, securityContextAccessor.currentUserId(), request));
    }

    @PostMapping("/change")
    public ApiResponse<SubscriptionResponse> changePlan(
            @PathVariable UUID organizationId,
            @Valid @RequestBody ChangePlanRequest request) {
        return ApiResponse.of(billingService.changePlan(
                organizationId, securityContextAccessor.currentUserId(), request));
    }
}
