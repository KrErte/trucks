package eu.fuelfleet.subscription.dto;

public record CheckoutRequest(
        String plan,
        String successUrl,
        String cancelUrl
) {
}
