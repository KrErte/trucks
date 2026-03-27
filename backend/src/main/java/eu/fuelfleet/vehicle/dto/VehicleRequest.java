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
        BigDecimal insurancePerDay,
        String axleConfiguration,
        Integer numberOfAxles,
        BigDecimal grossWeight,
        BigDecimal netWeight,
        Integer powerHp,
        Integer displacementCc,
        String gearbox,
        String suspension,
        String source,
        String sourceId,
        String sourceUrl
) {
    public VehicleRequest {
        if (fuelType == null || fuelType.isBlank()) {
            fuelType = "DIESEL";
        }
    }
}
