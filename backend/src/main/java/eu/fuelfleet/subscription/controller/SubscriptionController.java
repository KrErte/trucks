package eu.fuelfleet.subscription.controller;

import eu.fuelfleet.auth.security.UserPrincipal;
import eu.fuelfleet.subscription.dto.CheckoutRequest;
import eu.fuelfleet.subscription.dto.PortalRequest;
import eu.fuelfleet.subscription.entity.Subscription;
import eu.fuelfleet.subscription.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping("/api/subscriptions/checkout")
    public ResponseEntity<Map<String, String>> createCheckoutSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody CheckoutRequest request) {
        String sessionUrl = subscriptionService.createCheckoutSession(
                principal.getCompanyId(),
                principal.getEmail(),
                request.plan(),
                request.successUrl(),
                request.cancelUrl()
        );
        return ResponseEntity.ok(Map.of("sessionUrl", sessionUrl));
    }

    @PostMapping("/api/subscriptions/portal")
    public ResponseEntity<Map<String, String>> createPortalSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody PortalRequest request) {
        Subscription subscription = subscriptionService.getSubscription(principal.getCompanyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No subscription found"));

        if (subscription.getStripeCustomerId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No Stripe customer associated");
        }

        String portalUrl = subscriptionService.createPortalSession(
                subscription.getStripeCustomerId(),
                request.returnUrl()
        );
        return ResponseEntity.ok(Map.of("portalUrl", portalUrl));
    }

    @PostMapping("/api/webhooks/stripe")
    public ResponseEntity<Void> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        subscriptionService.handleWebhook(payload, sigHeader);
        return ResponseEntity.ok().build();
    }
}
