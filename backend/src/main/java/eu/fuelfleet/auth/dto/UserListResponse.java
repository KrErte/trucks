package eu.fuelfleet.auth.dto;

import eu.fuelfleet.auth.entity.User;
import java.time.LocalDateTime;
import java.util.UUID;

public record UserListResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String role,
        boolean active,
        LocalDateTime createdAt
) {
    public static UserListResponse fromEntity(User u) {
        return new UserListResponse(
                u.getId(), u.getEmail(), u.getFirstName(), u.getLastName(),
                u.getRole().name(), u.isActive(), u.getCreatedAt()
        );
    }
}
