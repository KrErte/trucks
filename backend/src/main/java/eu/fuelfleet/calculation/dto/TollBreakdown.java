package eu.fuelfleet.calculation.dto;

import java.math.BigDecimal;

public record TollBreakdown(
        String countryCode,
        BigDecimal distanceKm,
        BigDecimal costPerKm,
        BigDecimal cost
) {}
