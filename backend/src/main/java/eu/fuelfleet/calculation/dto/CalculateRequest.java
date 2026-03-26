package eu.fuelfleet.calculation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CalculateRequest(
        @NotNull UUID vehicleId,
        @NotBlank String originAddress,
        @NotBlank String destinationAddress,
        List<String> waypoints,
        BigDecimal cargoWeightTons,
        @NotNull BigDecimal orderPrice,
        String currency,
        boolean isLoaded,
        BigDecimal driverDailyRate,
        BigDecimal otherCosts,
        boolean includeReturnTrip
) {
    public CalculateRequest {
        if (currency == null || currency.isBlank()) {
            currency = "EUR";
        }
    }
}
