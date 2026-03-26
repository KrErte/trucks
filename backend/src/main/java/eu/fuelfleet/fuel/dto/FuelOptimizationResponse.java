package eu.fuelfleet.fuel.dto;

import java.math.BigDecimal;
import java.util.List;

public record FuelOptimizationResponse(
        List<FuelStopSuggestion> stops,
        BigDecimal totalOptimizedCost,
        BigDecimal totalNaiveCost,
        BigDecimal savings,
        BigDecimal savingsPercent
) {}
