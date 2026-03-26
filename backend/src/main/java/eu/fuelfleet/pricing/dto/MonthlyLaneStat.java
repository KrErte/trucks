package eu.fuelfleet.pricing.dto;

import java.math.BigDecimal;

public record MonthlyLaneStat(
        String yearMonth,
        int tripCount,
        BigDecimal avgPrice,
        BigDecimal avgMarginPct
) {}
