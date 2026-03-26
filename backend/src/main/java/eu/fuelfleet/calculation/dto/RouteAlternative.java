package eu.fuelfleet.calculation.dto;

import java.math.BigDecimal;

public record RouteAlternative(
        int index,
        String label,
        BigDecimal distanceKm,
        BigDecimal estimatedHours,
        String summary,
        BigDecimal estimatedFuelCost,
        BigDecimal estimatedTotalCost
) {}
