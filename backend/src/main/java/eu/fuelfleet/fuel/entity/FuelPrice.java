package eu.fuelfleet.fuel.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "fuel_prices")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FuelPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "country_code", nullable = false)
    private String countryCode;

    @Column(name = "fuel_type", nullable = false)
    private String fuelType;

    @Column(name = "price_per_liter", nullable = false)
    private BigDecimal pricePerLiter;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    private String source;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
