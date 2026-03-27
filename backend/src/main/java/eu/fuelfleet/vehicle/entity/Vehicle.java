package eu.fuelfleet.vehicle.entity;

import eu.fuelfleet.company.entity.Company;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "vehicles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "fuel_type", nullable = false)
    private FuelType fuelType;

    @Column(name = "consumption_loaded")
    private BigDecimal consumptionLoaded;

    @Column(name = "consumption_empty")
    private BigDecimal consumptionEmpty;

    @Column(name = "tank_capacity")
    private BigDecimal tankCapacity;

    @Column(name = "euro_class")
    private String euroClass;

    @Column(name = "maintenance_cost_per_km")
    private BigDecimal maintenanceCostPerKm;

    @Column(name = "tire_cost_per_km")
    private BigDecimal tireCostPerKm;

    @Column(name = "depreciation_per_km")
    private BigDecimal depreciationPerKm;

    @Column(name = "insurance_per_day")
    private BigDecimal insurancePerDay;

    @Column(name = "axle_configuration")
    private String axleConfiguration;

    @Column(name = "number_of_axles")
    private Integer numberOfAxles;

    @Column(name = "gross_weight")
    private BigDecimal grossWeight;

    @Column(name = "net_weight")
    private BigDecimal netWeight;

    @Column(name = "power_hp")
    private Integer powerHp;

    @Column(name = "displacement_cc")
    private Integer displacementCc;

    private String gearbox;

    private String suspension;

    @Column(length = 50)
    private String source;

    @Column(name = "source_id", length = 50)
    private String sourceId;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
