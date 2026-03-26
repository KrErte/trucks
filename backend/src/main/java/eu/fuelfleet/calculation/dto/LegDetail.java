package eu.fuelfleet.calculation.dto;

import java.math.BigDecimal;

public record LegDetail(String from, String to, BigDecimal distanceKm, BigDecimal estimatedHours) {
}
