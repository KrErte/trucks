package eu.fuelfleet.maintenance.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data @NoArgsConstructor @AllArgsConstructor
public class MaintenanceRequest {
    private UUID vehicleId;
    private String type;
    private String description;
    private BigDecimal cost;
    private Integer odometerKm;
    private LocalDate performedAt;
    private LocalDate nextDueDate;
    private Integer nextDueKm;
    private String performedBy;
    private String notes;
}
