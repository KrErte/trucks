package eu.fuelfleet.maintenance.repository;

import eu.fuelfleet.maintenance.entity.MaintenanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface MaintenanceRepository extends JpaRepository<MaintenanceRecord, UUID> {
    List<MaintenanceRecord> findByCompanyIdOrderByPerformedAtDesc(UUID companyId);
    List<MaintenanceRecord> findByVehicleIdOrderByPerformedAtDesc(UUID vehicleId);
    List<MaintenanceRecord> findByCompanyIdAndNextDueDateBeforeAndNextDueDateIsNotNull(UUID companyId, LocalDate date);
}
