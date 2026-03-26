package eu.fuelfleet.fuel.repository;

import eu.fuelfleet.fuel.entity.FuelPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FuelPriceRepository extends JpaRepository<FuelPrice, UUID> {

    @Query(value = "SELECT * FROM fuel_prices WHERE country_code = :countryCode AND fuel_type = :fuelType AND valid_from <= CURRENT_DATE ORDER BY valid_from DESC LIMIT 1", nativeQuery = true)
    Optional<FuelPrice> findCurrentPrice(@Param("countryCode") String countryCode, @Param("fuelType") String fuelType);

    List<FuelPrice> findByCountryCodeOrderByValidFromDesc(String countryCode);
}
