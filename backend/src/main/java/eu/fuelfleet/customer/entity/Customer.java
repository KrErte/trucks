package eu.fuelfleet.customer.entity;

import eu.fuelfleet.company.entity.Company;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "customers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Customer {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false)
    private String name;

    @Column(name = "vat_number")
    private String vatNumber;

    @Column(name = "reg_code")
    private String regCode;

    private String email;
    private String phone;
    private String address;
    private String country;

    @Column(name = "contact_person")
    private String contactPerson;

    @Column(name = "payment_term_days")
    private Integer paymentTermDays;

    private String notes;
    private boolean active = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        if (paymentTermDays == null) paymentTermDays = 14;
    }
}
