package eu.fuelfleet.auth.dto;

import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String email,
        String role,
        UUID companyId
) {}
