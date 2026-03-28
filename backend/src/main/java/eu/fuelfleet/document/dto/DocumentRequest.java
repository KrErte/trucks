package eu.fuelfleet.document.dto;

import lombok.*;
import java.time.LocalDate;
import java.util.UUID;

@Data @NoArgsConstructor @AllArgsConstructor
public class DocumentRequest {
    private UUID vehicleId;
    private UUID driverId;
    private String type;
    private String name;
    private LocalDate expiryDate;
    private LocalDate issueDate;
    private String documentNumber;
    private String notes;
}
