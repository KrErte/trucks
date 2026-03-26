package eu.fuelfleet.subscription.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import eu.fuelfleet.subscription.entity.Plan;
import eu.fuelfleet.subscription.entity.Subscription;
import eu.fuelfleet.subscription.entity.SubscriptionStatus;
import eu.fuelfleet.subscription.repository.SubscriptionRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;

    @Value("${app.stripe.secret-key:}")
    private String stripeSecretKey;

    @Value("${app.stripe.webhook-secret:}")
    private String stripeWebhookSecret;
    private static final Map<String, String> PLAN_PRICE_IDS = Map.of(
            "STARTER", "price_starter",
            "GROWTH", "price_growth",
            "ENTERPRISE", "price_enterprise"
    );

    @PostConstruct
    public void init() {
        if (stripeSecretKey != null && !stripeSecretKey.isBlank()) {
            Stripe.apiKey = stripeSecretKey;
        }
    }

    @Transactional(readOnly = true)
    public Optional<Subscription> getSubscription(UUID companyId) {
        return subscriptionRepository.findByCompanyId(companyId);
    }

    @Transactional
    public String getOrCreateStripeCustomer(UUID companyId, String email) {
        Optional<Subscription> existing = subscriptionRepository.findByCompanyId(companyId);
        if (existing.isPresent() && existing.get().getStripeCustomerId() != null) {
            return existing.get().getStripeCustomerId();
        }

        try {
            CustomerCreateParams params = CustomerCreateParams.builder()
                    .setEmail(email)
                    .putMetadata("companyId", companyId.toString())
                    .build();
            Customer customer = Customer.create(params);
            return customer.getId();
        } catch (StripeException e) {
            log.error("Failed to create Stripe customer", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create Stripe customer");
        }
    }

    @Transactional
    public String createCheckoutSession(UUID companyId, String email, String plan, String successUrl, String cancelUrl) {
        String customerId = getOrCreateStripeCustomer(companyId, email);
        String priceId = PLAN_PRICE_IDS.getOrDefault(plan.toUpperCase(), "price_starter");

        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setCustomer(customerId)
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setSuccessUrl(successUrl)
                    .setCancelUrl(cancelUrl)
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setPrice(priceId)
                            .setQuantity(1L)
                            .build())
                    .putMetadata("companyId", companyId.toString())
                    .putMetadata("plan", plan.toUpperCase())
                    .build();

            Session session = Session.create(params);
            return session.getUrl();
        } catch (StripeException e) {
            log.error("Failed to create checkout session", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create checkout session");
        }
    }

    public String createPortalSession(String stripeCustomerId, String returnUrl) {
        try {
            com.stripe.param.billingportal.SessionCreateParams params =
                    com.stripe.param.billingportal.SessionCreateParams.builder()
                            .setCustomer(stripeCustomerId)
                            .setReturnUrl(returnUrl)
                            .build();

            com.stripe.model.billingportal.Session session =
                    com.stripe.model.billingportal.Session.create(params);
            return session.getUrl();
        } catch (StripeException e) {
            log.error("Failed to create portal session", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create portal session");
        }
    }

    @Transactional
    public void handleWebhook(String payload, String sigHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, stripeWebhookSecret);
        } catch (Exception e) {
            log.error("Webhook signature verification failed", e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid webhook signature");
        }

        switch (event.getType()) {
            case "checkout.session.completed" -> handleCheckoutCompleted(event);
            case "customer.subscription.updated" -> handleSubscriptionUpdated(event);
            case "customer.subscription.deleted" -> handleSubscriptionDeleted(event);
            default -> log.info("Unhandled event type: {}", event.getType());
        }
    }

    private void handleCheckoutCompleted(Event event) {
        Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
        if (session == null) return;

        String companyIdStr = session.getMetadata().get("companyId");
        String plan = session.getMetadata().get("plan");
        UUID companyId = UUID.fromString(companyIdStr);

        Subscription subscription = subscriptionRepository.findByCompanyId(companyId)
                .orElse(Subscription.builder().build());

        subscription.setStripeCustomerId(session.getCustomer());
        subscription.setStripeSubscriptionId(session.getSubscription());
        subscription.setPlan(Plan.valueOf(plan));
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscriptionRepository.save(subscription);
    }

    private void handleSubscriptionUpdated(Event event) {
        com.stripe.model.Subscription stripeSub =
                (com.stripe.model.Subscription) event.getDataObjectDeserializer().getObject().orElse(null);
        if (stripeSub == null) return;

        subscriptionRepository.findByStripeSubscriptionId(stripeSub.getId())
                .ifPresent(sub -> {
                    String status = stripeSub.getStatus();
                    switch (status) {
                        case "active" -> sub.setStatus(SubscriptionStatus.ACTIVE);
                        case "past_due" -> sub.setStatus(SubscriptionStatus.PAST_DUE);
                        case "canceled" -> sub.setStatus(SubscriptionStatus.CANCELED);
                        case "trialing" -> sub.setStatus(SubscriptionStatus.TRIALING);
                    }
                    subscriptionRepository.save(sub);
                });
    }

    private void handleSubscriptionDeleted(Event event) {
        com.stripe.model.Subscription stripeSub =
                (com.stripe.model.Subscription) event.getDataObjectDeserializer().getObject().orElse(null);
        if (stripeSub == null) return;

        subscriptionRepository.findByStripeSubscriptionId(stripeSub.getId())
                .ifPresent(sub -> {
                    sub.setStatus(SubscriptionStatus.CANCELED);
                    subscriptionRepository.save(sub);
                });
    }
}
