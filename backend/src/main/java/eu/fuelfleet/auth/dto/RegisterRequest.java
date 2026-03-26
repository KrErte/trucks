package eu.fuelfleet.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @NotBlank String email,
        @NotBlank String password,
        String firstName,
        String lastName,
        @NotBlank String companyName,
        String vatNumber,
        String country,
        String regCode
) {}
