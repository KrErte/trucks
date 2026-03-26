package eu.fuelfleet.pricing.dto;

import java.math.BigDecimal;
import java.util.List;

public record LaneIntelligence(
        int tripCount,
        BigDecimal avgPrice,
        BigDecimal avgMarginPct,
        BigDecimal avgRevenuePerKm,
        String priceTrend,
        BigDecimal bestTripMarginPct,
        BigDecimal worstTripMarginPct,
        List<MonthlyLaneStat> monthlyBreakdown,
        String matchType
) {}
