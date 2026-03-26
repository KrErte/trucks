package eu.fuelfleet.vehicle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record VehicleRequest(
        @NotBlank String name,
        String fuelType,
        @NotNull BigDecimal consumptionLoaded,
        @NotNull BigDecimal consumptionEmpty,
        BigDecimal tankCapacity,
        String euroClass,
        BigDecimal maintenanceCostPerKm,
        BigDecimal tireCostPerKm,
        BigDecimal depreciationPerKm,
        BigDecimal insurancePerDay
) {
    public VehicleRequest {
        if (fuelType == null || fuelType.isBlank()) {
            fuelType = "DIESEL";
        }
    }
}
