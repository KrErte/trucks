package eu.fuelfleet.auth.dto;

import eu.fuelfleet.auth.entity.AuditLog;
import java.time.LocalDateTime;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        UUID userId,
        String action,
        String entityType,
        String entityId,
        String details,
        String ipAddress,
        LocalDateTime createdAt
) {
    public static AuditLogResponse fromEntity(AuditLog a) {
        return new AuditLogResponse(
                a.getId(), a.getUserId(), a.getAction(), a.getEntityType(),
                a.getEntityId(), a.getDetails(), a.getIpAddress(), a.getCreatedAt()
        );
    }
}
