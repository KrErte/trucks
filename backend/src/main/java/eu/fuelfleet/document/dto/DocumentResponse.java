package eu.fuelfleet.document.dto;

import eu.fuelfleet.document.entity.VehicleDocument;
import lombok.*;
import java.time.LocalDate;
import java.util.UUID;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class DocumentResponse {
    private UUID id;
    private UUID vehicleId;
    private String vehicleName;
    private UUID driverId;
    private String driverName;
    private String type;
    private String name;
    private LocalDate expiryDate;
    private LocalDate issueDate;
    private String documentNumber;
    private String notes;

    public static DocumentResponse from(VehicleDocument d) {
        return DocumentResponse.builder()
                .id(d.getId())
                .vehicleId(d.getVehicle() != null ? d.getVehicle().getId() : null)
                .vehicleName(d.getVehicle() != null ? d.getVehicle().getName() : null)
                .driverId(d.getDriver() != null ? d.getDriver().getId() : null)
                .driverName(d.getDriver() != null ? d.getDriver().getFirstName() + " " + d.getDriver().getLastName() : null)
                .type(d.getType()).name(d.getName())
                .expiryDate(d.getExpiryDate()).issueDate(d.getIssueDate())
                .documentNumber(d.getDocumentNumber()).notes(d.getNotes())
                .build();
    }
}
