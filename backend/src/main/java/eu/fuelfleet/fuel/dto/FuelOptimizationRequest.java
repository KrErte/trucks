package eu.fuelfleet.fuel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record FuelOptimizationRequest(
        @NotNull UUID vehicleId,
        @NotBlank String originAddress,
        @NotBlank String destinationAddress,
        @NotNull BigDecimal distanceKm,
        BigDecimal currentFuelLiters,
        boolean isLoaded
) {}
