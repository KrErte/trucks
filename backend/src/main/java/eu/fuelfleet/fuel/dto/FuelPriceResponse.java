package eu.fuelfleet.fuel.dto;

import eu.fuelfleet.fuel.entity.FuelPrice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record FuelPriceResponse(
        UUID id,
        String countryCode,
        String fuelType,
        BigDecimal pricePerLiter,
        LocalDate validFrom,
        String source,
        LocalDateTime createdAt
) {
    public static FuelPriceResponse fromEntity(FuelPrice f) {
        return new FuelPriceResponse(
                f.getId(),
                f.getCountryCode(),
                f.getFuelType(),
                f.getPricePerLiter(),
                f.getValidFrom(),
                f.getSource(),
                f.getCreatedAt()
        );
    }
}
