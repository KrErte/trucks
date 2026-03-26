package eu.fuelfleet.pricing.dto;

import java.math.BigDecimal;

public record MarketContext(
        BigDecimal companyAvgMarginPct,
        BigDecimal companyAvgRevenuePerKm,
        BigDecimal laneVsCompanyMarginDelta,
        String seasonalPattern
) {}
