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
                v.getInsurancePerDay(),
                v.getAxleConfiguration(),
                v.getNumberOfAxles(),
                v.getGrossWeight(),
                v.getNetWeight(),
                v.getPowerHp(),
                v.getDisplacementCc(),
                v.getGearbox(),
                v.getSuspension(),
                v.getSource(),
                v.getSourceId(),
                v.getSourceUrl()
        );
    }
}
