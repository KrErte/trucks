package eu.fuelfleet.auth.dto;

import java.time.LocalDateTime;

public record ProfileResponse(
        String firstName,
        String lastName,
        String email,
        String role,
        LocalDateTime createdAt
) {
}
