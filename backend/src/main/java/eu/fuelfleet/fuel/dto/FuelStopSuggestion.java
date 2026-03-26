package eu.fuelfleet.fuel.dto;

import java.math.BigDecimal;

public record FuelStopSuggestion(
        String country,
        String countryName,
        BigDecimal fuelPricePerLiter,
        BigDecimal litersToFill,
        BigDecimal cost,
        String reason
) {}
