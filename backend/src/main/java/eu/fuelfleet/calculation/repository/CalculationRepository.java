package eu.fuelfleet.calculation.repository;

import eu.fuelfleet.calculation.entity.Calculation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import java.util.UUID;

@Repository
public interface CalculationRepository extends JpaRepository<Calculation, UUID> {

    @EntityGraph(attributePaths = {"vehicle"})
    Optional<Calculation> findById(UUID id);

    @EntityGraph(attributePaths = {"vehicle"})
    Page<Calculation> findByCompanyIdOrderByCreatedAtDesc(UUID companyId, Pageable pageable);

    @EntityGraph(attributePaths = {"vehicle"})
    List<Calculation> findByCompanyId(UUID companyId);

    @EntityGraph(attributePaths = {"vehicle"})
    List<Calculation> findByCompanyIdAndCreatedAtAfter(UUID companyId, LocalDateTime since);

    @EntityGraph(attributePaths = {"vehicle"})
    List<Calculation> findByCompanyIdAndCreatedAtBetween(UUID companyId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT c.origin, c.destination, COUNT(c), AVG(c.profit), AVG(c.profitMarginPct), " +
           "SUM(c.orderPrice), SUM(c.totalCost) " +
           "FROM Calculation c WHERE c.company.id = :companyId " +
           "GROUP BY c.origin, c.destination ORDER BY COUNT(c) DESC")
    List<Object[]> findLaneStats(@Param("companyId") UUID companyId);

    @EntityGraph(attributePaths = {"vehicle"})
    @Query("SELECT c FROM Calculation c WHERE c.company.id = :companyId " +
           "AND LOWER(c.origin) LIKE LOWER(CONCAT('%', :originKey, '%')) " +
           "AND LOWER(c.destination) LIKE LOWER(CONCAT('%', :destKey, '%')) " +
           "ORDER BY c.createdAt DESC")
    List<Calculation> findByLane(@Param("companyId") UUID companyId,
                                 @Param("originKey") String originKey,
                                 @Param("destKey") String destKey);

    @EntityGraph(attributePaths = {"vehicle"})
    @Query("SELECT c FROM Calculation c WHERE c.company.id = :companyId " +
           "AND LOWER(c.origin) LIKE LOWER(CONCAT('%', :originCountry, '%')) " +
           "AND LOWER(c.destination) LIKE LOWER(CONCAT('%', :destCountry, '%')) " +
           "AND c.distanceKm BETWEEN :minDist AND :maxDist " +
           "ORDER BY c.createdAt DESC")
    List<Calculation> findByCountryPairAndDistance(@Param("companyId") UUID companyId,
                                                   @Param("originCountry") String originCountry,
                                                   @Param("destCountry") String destCountry,
                                                   @Param("minDist") BigDecimal minDist,
                                                   @Param("maxDist") BigDecimal maxDist);
}
