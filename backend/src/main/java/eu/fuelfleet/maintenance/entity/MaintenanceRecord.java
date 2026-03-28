package eu.fuelfleet.maintenance.entity;

import eu.fuelfleet.company.entity.Company;
import eu.fuelfleet.vehicle.entity.Vehicle;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "maintenance_records")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MaintenanceRecord {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false)
    private String type;

    private String description;
    private BigDecimal cost;

    @Column(name = "odometer_km")
    private Integer odometerKm;

    @Column(name = "performed_at", nullable = false)
    private LocalDate performedAt;

    @Column(name = "next_due_date")
    private LocalDate nextDueDate;

    @Column(name = "next_due_km")
    private Integer nextDueKm;

    @Column(name = "performed_by")
    private String performedBy;

    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() { createdAt = LocalDateTime.now(); }
}
