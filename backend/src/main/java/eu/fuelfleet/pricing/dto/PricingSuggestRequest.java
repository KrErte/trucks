package eu.fuelfleet.pricing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record PricingSuggestRequest(
        @NotBlank String originAddress,
        @NotBlank String destinationAddress,
        @NotNull UUID vehicleId,
        BigDecimal cargoWeightTons,
        boolean includeReturnTrip
) {}
