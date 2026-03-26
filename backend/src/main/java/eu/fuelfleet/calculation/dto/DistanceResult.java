package eu.fuelfleet.calculation.dto;

import java.math.BigDecimal;

public record DistanceResult(BigDecimal distanceKm, BigDecimal estimatedHours, String summary) {
    public DistanceResult(BigDecimal distanceKm, BigDecimal estimatedHours) {
        this(distanceKm, estimatedHours, null);
    }
}
