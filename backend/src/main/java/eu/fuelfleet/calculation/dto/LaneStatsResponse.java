package eu.fuelfleet.calculation.dto;

import java.math.BigDecimal;

public record LaneStatsResponse(
        String origin,
        String destination,
        long tripCount,
        BigDecimal avgProfit,
        BigDecimal avgMargin,
        BigDecimal totalRevenue,
        BigDecimal totalCost
) {}
