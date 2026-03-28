package eu.fuelfleet.document.entity;

import eu.fuelfleet.company.entity.Company;
import eu.fuelfleet.driver.entity.Driver;
import eu.fuelfleet.vehicle.entity.Vehicle;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "vehicle_documents")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VehicleDocument {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private Driver driver;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String name;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "document_number")
    private String documentNumber;

    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() { createdAt = LocalDateTime.now(); }
}
