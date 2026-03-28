package eu.fuelfleet.driver.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data @NoArgsConstructor @AllArgsConstructor
public class DriverRequest {
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
    private String notes;
}
