package eu.fuelfleet.route.repository;

import eu.fuelfleet.route.entity.SavedRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SavedRouteRepository extends JpaRepository<SavedRoute, UUID> {
    List<SavedRoute> findByCompanyIdOrderByNameAsc(UUID companyId);
}
