package eu.fuelfleet.route.dto;

import jakarta.validation.constraints.NotBlank;

public record SavedRouteRequest(
        @NotBlank String name,
        @NotBlank String originAddress,
        @NotBlank String destinationAddress
) {}
