package eu.fuelfleet.maintenance.dto;

import eu.fuelfleet.maintenance.entity.MaintenanceRecord;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class MaintenanceResponse {
    private UUID id;
    private UUID vehicleId;
    private String vehicleName;
    private String type;
    private String description;
    private BigDecimal cost;
    private Integer odometerKm;
    private LocalDate performedAt;
    private LocalDate nextDueDate;
    private Integer nextDueKm;
    private String performedBy;
    private String notes;

    public static MaintenanceResponse from(MaintenanceRecord m) {
        return MaintenanceResponse.builder()
                .id(m.getId()).vehicleId(m.getVehicle().getId())
                .vehicleName(m.getVehicle().getName()).type(m.getType())
                .description(m.getDescription()).cost(m.getCost())
                .odometerKm(m.getOdometerKm()).performedAt(m.getPerformedAt())
                .nextDueDate(m.getNextDueDate()).nextDueKm(m.getNextDueKm())
                .performedBy(m.getPerformedBy()).notes(m.getNotes())
                .build();
    }
}
