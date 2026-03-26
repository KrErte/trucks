package eu.fuelfleet.vehicle.dto;

import eu.fuelfleet.vehicle.entity.Vehicle;

import java.math.BigDecimal;
import java.util.UUID;

public record VehicleResponse(
        UUID id,
        String name,
        String fuelType,
        BigDecimal consumptionLoaded,
        BigDecimal consumptionEmpty,
        BigDecimal tankCapacity,
        String euroClass,
        boolean active,
        BigDecimal maintenanceCostPerKm,
        BigDecimal tireCostPerKm,
        BigDecimal depreciationPerKm,
        BigDecimal insurancePerDay
) {
    public static VehicleResponse fromEntity(Vehicle v) {
        return new VehicleResponse(
                v.getId(),
                v.getName(),
                v.getFuelType().name(),
                v.getConsumptionLoaded(),
                v.getConsumptionEmpty(),
                v.getTankCapacity(),
                v.getEuroClass(),
                v.isActive(),
                v.getMaintenanceCostPerKm(),
                v.getTireCostPerKm(),
                v.getDepreciationPerKm(),
                v.getInsurancePerDay()
        );
    }
}
