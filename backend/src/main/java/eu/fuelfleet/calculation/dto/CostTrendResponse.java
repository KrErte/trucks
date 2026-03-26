package eu.fuelfleet.calculation.dto;

import java.math.BigDecimal;

public record CostTrendResponse(
        String month,
        BigDecimal avgFuelCost,
        BigDecimal avgTollCost,
        BigDecimal avgDriverCost,
        BigDecimal avgTotalCost,
        BigDecimal avgRevenue,
        BigDecimal avgProfit,
        long tripCount
) {}
