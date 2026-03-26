package eu.fuelfleet.calculation.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.fuelfleet.calculation.entity.Calculation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public record CalculationResponse(
        UUID id,
        String origin,
        String destination,
        BigDecimal distanceKm,
        BigDecimal estimatedHours,
        String vehicleName,
        BigDecimal cargoWeightT,
        BigDecimal orderPrice,
        String currency,
        BigDecimal fuelCost,
        BigDecimal tollCost,
        BigDecimal driverDailyCost,
        BigDecimal otherCosts,
        BigDecimal totalCost,
        BigDecimal profit,
        BigDecimal profitMarginPct,
        BigDecimal revenuePerKm,
        boolean isProfitable,
        String fuelBreakdown,
        boolean includeReturnTrip,
        BigDecimal returnFuelCost,
        BigDecimal co2EmissionsKg,
        int driverDays,
        BigDecimal costPerKm,
        BigDecimal maintenanceCost,
        BigDecimal tireCost,
        BigDecimal depreciationCost,
        BigDecimal insuranceCost,
        Integer routeIndex,
        String routeLabel,
        List<TollBreakdown> tollBreakdown,
        List<String> waypoints,
        List<LegDetail> legs,
        LocalDateTime createdAt
) {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static CalculationResponse fromEntity(Calculation c) {
        List<String> wp = Collections.emptyList();
        List<LegDetail> legs = Collections.emptyList();
        List<TollBreakdown> tollBreakdown = Collections.emptyList();
        if (c.getWaypoints() != null && !c.getWaypoints().isBlank()) {
            try {
                wp = MAPPER.readValue(c.getWaypoints(), new TypeReference<List<String>>() {});
            } catch (Exception ignored) {}
        }
        if (c.getFuelBreakdown() != null && !c.getFuelBreakdown().isBlank()) {
            try {
                legs = MAPPER.readValue(c.getFuelBreakdown(), new TypeReference<List<LegDetail>>() {});
            } catch (Exception ignored) {}
        }
        if (c.getTollBreakdown() != null && !c.getTollBreakdown().isBlank()) {
            try {
                tollBreakdown = MAPPER.readValue(c.getTollBreakdown(), new TypeReference<List<TollBreakdown>>() {});
            } catch (Exception ignored) {}
        }
        return new CalculationResponse(
                c.getId(),
                c.getOrigin(),
                c.getDestination(),
                c.getDistanceKm(),
                c.getEstimatedHours(),
                c.getVehicle().getName(),
                c.getCargoWeightT(),
                c.getOrderPrice(),
                c.getCurrency(),
                c.getFuelCost(),
                c.getTollCost(),
                c.getDriverDailyCost(),
                c.getOtherCosts(),
                c.getTotalCost(),
                c.getProfit(),
                c.getProfitMarginPct(),
                c.getRevenuePerKm(),
                c.getProfit() != null && c.getProfit().signum() > 0,
                c.getFuelBreakdown(),
                c.isIncludeReturnTrip(),
                c.getReturnFuelCost() != null ? c.getReturnFuelCost() : BigDecimal.ZERO,
                c.getCo2EmissionsKg() != null ? c.getCo2EmissionsKg() : BigDecimal.ZERO,
                c.getDriverDays() != null ? c.getDriverDays() : 0,
                c.getCostPerKm() != null ? c.getCostPerKm() : BigDecimal.ZERO,
                c.getMaintenanceCost() != null ? c.getMaintenanceCost() : BigDecimal.ZERO,
                c.getTireCost() != null ? c.getTireCost() : BigDecimal.ZERO,
                c.getDepreciationCost() != null ? c.getDepreciationCost() : BigDecimal.ZERO,
                c.getInsuranceCost() != null ? c.getInsuranceCost() : BigDecimal.ZERO,
                c.getRouteIndex(),
                c.getRouteLabel(),
                tollBreakdown,
                wp,
                legs,
                c.getCreatedAt()
        );
    }
}
