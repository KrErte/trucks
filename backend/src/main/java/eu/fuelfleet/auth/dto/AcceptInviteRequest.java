package eu.fuelfleet.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record AcceptInviteRequest(
        @NotBlank String token,
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank String password
) {}
