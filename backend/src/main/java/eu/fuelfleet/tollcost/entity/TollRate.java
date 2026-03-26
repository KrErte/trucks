package eu.fuelfleet.tollcost.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "toll_rates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TollRate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "country_code", nullable = false)
    private String countryCode;

    @Column(name = "vehicle_class", nullable = false)
    private String vehicleClass;

    @Column(name = "cost_per_km", nullable = false)
    private BigDecimal costPerKm;

    @Column(nullable = false)
    private String currency;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
