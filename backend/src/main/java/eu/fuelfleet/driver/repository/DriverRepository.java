package eu.fuelfleet.driver.repository;

import eu.fuelfleet.driver.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DriverRepository extends JpaRepository<Driver, UUID> {
    List<Driver> findByCompanyIdAndActiveTrue(UUID companyId);
    List<Driver> findByCompanyId(UUID companyId);
    List<Driver> findByLicenseExpiryBeforeAndActiveTrue(LocalDate date);
    List<Driver> findByDriverCardExpiryBeforeAndActiveTrue(LocalDate date);
    List<Driver> findByAdrCertificateExpiryBeforeAndActiveTrue(LocalDate date);
}
