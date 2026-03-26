package eu.fuelfleet.tollcost.dto;

import eu.fuelfleet.tollcost.entity.TollRate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record TollRateResponse(
        UUID id,
        String countryCode,
        String vehicleClass,
        BigDecimal costPerKm,
        String currency,
        LocalDate validFrom,
        LocalDateTime createdAt
) {
    public static TollRateResponse fromEntity(TollRate t) {
        return new TollRateResponse(
                t.getId(),
                t.getCountryCode(),
                t.getVehicleClass(),
                t.getCostPerKm(),
                t.getCurrency(),
                t.getValidFrom(),
                t.getCreatedAt()
        );
    }
}
