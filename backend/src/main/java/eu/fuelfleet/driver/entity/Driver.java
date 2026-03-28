package eu.fuelfleet.driver.entity;

import eu.fuelfleet.company.entity.Company;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "drivers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Driver {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    private String email;
    private String phone;

    @Column(name = "license_number")
    private String licenseNumber;

    @Column(name = "license_expiry")
    private LocalDate licenseExpiry;

    @Column(name = "license_categories")
    private String licenseCategories;

    @Column(name = "id_card_number")
    private String idCardNumber;

    @Column(name = "id_card_expiry")
    private LocalDate idCardExpiry;

    @Column(name = "adr_certificate_expiry")
    private LocalDate adrCertificateExpiry;

    @Column(name = "driver_card_number")
    private String driverCardNumber;

    @Column(name = "driver_card_expiry")
    private LocalDate driverCardExpiry;

    @Column(name = "daily_rate")
    private BigDecimal dailyRate;

    private boolean active = true;
    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        if (dailyRate == null) dailyRate = BigDecimal.ZERO;
    }
}
