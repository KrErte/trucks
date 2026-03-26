package eu.fuelfleet.pricing.dto;

import java.math.BigDecimal;

public record SuggestedPriceRange(
        BigDecimal estimatedTotalCost,
        BigDecimal minPrice,
        BigDecimal optimalPrice,
        BigDecimal premiumPrice,
        BigDecimal distanceKm,
        BigDecimal costPerKm,
        BigDecimal minRevenuePerKm,
        BigDecimal optimalRevenuePerKm,
        BigDecimal premiumRevenuePerKm
) {}
