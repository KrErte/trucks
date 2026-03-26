package eu.fuelfleet.calculation.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ConfirmRouteRequest(
        @NotNull UUID vehicleId,
        String originAddress,
        String destinationAddress,
        List<String> waypoints,
        BigDecimal cargoWeightTons,
        @NotNull BigDecimal orderPrice,
        String currency,
        boolean isLoaded,
        BigDecimal driverDailyRate,
        BigDecimal otherCosts,
        boolean includeReturnTrip,
        int routeIndex,
        BigDecimal distanceKm,
        BigDecimal estimatedHours,
        String routeLabel
) {}
