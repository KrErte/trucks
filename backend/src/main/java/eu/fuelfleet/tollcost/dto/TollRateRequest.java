package eu.fuelfleet.tollcost.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record TollRateRequest(
        @NotBlank String countryCode,
        @NotBlank String vehicleClass,
        @NotNull BigDecimal costPerKm,
        String currency
) {
}
