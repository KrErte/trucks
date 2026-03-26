package eu.fuelfleet.vehicle.repository;

import eu.fuelfleet.vehicle.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {

    List<Vehicle> findByCompanyIdAndActiveTrue(UUID companyId);

    long countByCompanyIdAndActiveTrue(UUID companyId);
}
