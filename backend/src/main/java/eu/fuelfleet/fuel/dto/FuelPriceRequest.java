package eu.fuelfleet.fuel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record FuelPriceRequest(
        @NotBlank String countryCode,
        String fuelType,
        @NotNull BigDecimal pricePerLiter,
        String source
) {
}
