package eu.fuelfleet.document.repository;

import eu.fuelfleet.document.entity.VehicleDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface VehicleDocumentRepository extends JpaRepository<VehicleDocument, UUID> {
    List<VehicleDocument> findByCompanyIdOrderByExpiryDateAsc(UUID companyId);
    List<VehicleDocument> findByVehicleIdOrderByExpiryDateAsc(UUID vehicleId);
    List<VehicleDocument> findByDriverIdOrderByExpiryDateAsc(UUID driverId);
    List<VehicleDocument> findByCompanyIdAndExpiryDateBeforeAndExpiryDateIsNotNull(UUID companyId, LocalDate date);
}
