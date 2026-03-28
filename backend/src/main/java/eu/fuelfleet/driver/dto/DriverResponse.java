package eu.fuelfleet.driver.dto;

import eu.fuelfleet.driver.entity.Driver;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class DriverResponse {
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String licenseNumber;
    private LocalDate licenseExpiry;
    private String licenseCategories;
    private String idCardNumber;
    private LocalDate idCardExpiry;
    private LocalDate adrCertificateExpiry;
    private String driverCardNumber;
    private LocalDate driverCardExpiry;
    private BigDecimal dailyRate;
    private boolean active;
    private String notes;

    public static DriverResponse from(Driver d) {
        return DriverResponse.builder()
                .id(d.getId())
                .firstName(d.getFirstName())
                .lastName(d.getLastName())
                .email(d.getEmail())
                .phone(d.getPhone())
                .licenseNumber(d.getLicenseNumber())
                .licenseExpiry(d.getLicenseExpiry())
                .licenseCategories(d.getLicenseCategories())
                .idCardNumber(d.getIdCardNumber())
                .idCardExpiry(d.getIdCardExpiry())
                .adrCertificateExpiry(d.getAdrCertificateExpiry())
                .driverCardNumber(d.getDriverCardNumber())
                .driverCardExpiry(d.getDriverCardExpiry())
                .dailyRate(d.getDailyRate())
                .active(d.isActive())
                .notes(d.getNotes())
                .build();
    }
}
