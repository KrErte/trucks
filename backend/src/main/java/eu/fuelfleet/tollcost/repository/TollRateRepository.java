package eu.fuelfleet.tollcost.repository;

import eu.fuelfleet.tollcost.entity.TollRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TollRateRepository extends JpaRepository<TollRate, UUID> {

    @Query(value = "SELECT * FROM toll_rates WHERE country_code = :countryCode AND vehicle_class = :vehicleClass AND valid_from <= CURRENT_DATE ORDER BY valid_from DESC LIMIT 1", nativeQuery = true)
    Optional<TollRate> findCurrentRate(@Param("countryCode") String countryCode, @Param("vehicleClass") String vehicleClass);
}
