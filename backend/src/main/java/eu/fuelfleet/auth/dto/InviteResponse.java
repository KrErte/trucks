package eu.fuelfleet.auth.dto;

import eu.fuelfleet.auth.entity.UserInvitation;
import java.time.LocalDateTime;
import java.util.UUID;

public record InviteResponse(
        UUID id,
        String email,
        String role,
        LocalDateTime expiresAt,
        boolean accepted,
        LocalDateTime createdAt
) {
    public static InviteResponse fromEntity(UserInvitation inv) {
        return new InviteResponse(
                inv.getId(), inv.getEmail(), inv.getRole().name(),
                inv.getExpiresAt(), inv.isAccepted(), inv.getCreatedAt()
        );
    }
}
