package eu.fuelfleet.pricing.service;

import eu.fuelfleet.calculation.dto.DistanceResult;
import eu.fuelfleet.calculation.entity.Calculation;
import eu.fuelfleet.calculation.repository.CalculationRepository;
import eu.fuelfleet.calculation.service.GoogleMapsService;
import eu.fuelfleet.company.entity.Company;
import eu.fuelfleet.company.service.CompanyService;
import eu.fuelfleet.fuel.entity.FuelPrice;
import eu.fuelfleet.fuel.service.FuelPriceService;
import eu.fuelfleet.pricing.dto.*;
import eu.fuelfleet.tollcost.entity.TollRate;
import eu.fuelfleet.tollcost.repository.TollRateRepository;
import eu.fuelfleet.vehicle.entity.Vehicle;
import eu.fuelfleet.vehicle.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PricingService {

    private final CalculationRepository calculationRepository;
    private final VehicleService vehicleService;
    private final FuelPriceService fuelPriceService;
    private final TollRateRepository tollRateRepository;
    private final GoogleMapsService googleMapsService;
    private final CompanyService companyService;

    @Transactional(readOnly = true)
    public PricingSuggestionResponse suggest(PricingSuggestRequest request, UUID companyId) {
        Vehicle vehicle = vehicleService.getVehicle(request.vehicleId(), companyId);
        Company company = companyService.getCompany(companyId);

        // 1. Get distance via Google Maps
        List<String> addresses = List.of(request.originAddress(), request.destinationAddress());
        List<DistanceResult> legs = googleMapsService.getMultiLegDistances(addresses);
        BigDecimal distanceKm = legs.stream()
                .map(DistanceResult::distanceKm)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal estimatedHours = legs.stream()
                .map(DistanceResult::estimatedHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2. Calculate current cost (same logic as CalculationService)
        String countryCode = (company.getCountry() != null && !company.getCountry().isBlank())
                ? company.getCountry() : "EE";
        FuelPrice fuelPrice = fuelPriceService.getCurrentPrice(countryCode, vehicle.getFuelType().name());

        BigDecimal consumption = vehicle.getConsumptionLoaded();
        BigDecimal fuelCost = distanceKm
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
                .multiply(consumption)
                .multiply(fuelPrice.getPricePerLiter())
                .setScale(2, RoundingMode.HALF_UP);

        // Toll cost
        Map<String, BigDecimal> countryDistances = googleMapsService.getCountryDistances(
                request.originAddress(), request.destinationAddress());
        BigDecimal tollCost = BigDecimal.ZERO;
        if (!countryDistances.isEmpty()) {
            for (Map.Entry<String, BigDecimal> entry : countryDistances.entrySet()) {
                Optional<TollRate> rateOpt = tollRateRepository.findCurrentRate(entry.getKey(), vehicle.getEuroClass());
                if (rateOpt.isPresent()) {
                    tollCost = tollCost.add(entry.getValue().multiply(rateOpt.get().getCostPerKm())
                            .setScale(2, RoundingMode.HALF_UP));
                }
            }
        } else {
            tollCost = tollRateRepository.findCurrentRate(countryCode, vehicle.getEuroClass())
                    .map(tr -> distanceKm.multiply(tr.getCostPerKm()).setScale(2, RoundingMode.HALF_UP))
                    .orElse(BigDecimal.ZERO);
        }

        // Driver cost
        BigDecimal driverDailyRate = company.getDefaultDriverDailyRate() != null
                ? company.getDefaultDriverDailyRate() : BigDecimal.ZERO;
        long drivingDays = Math.max(1, estimatedHours
                .divide(BigDecimal.valueOf(24), 0, RoundingMode.CEILING).longValue());
        BigDecimal driverCost = driverDailyRate.multiply(BigDecimal.valueOf(drivingDays))
                .setScale(2, RoundingMode.HALF_UP);

        // Per-km costs
        BigDecimal maintenanceCost = safePerKm(vehicle.getMaintenanceCostPerKm(), distanceKm);
        BigDecimal tireCost = safePerKm(vehicle.getTireCostPerKm(), distanceKm);
        BigDecimal depreciationCost = safePerKm(vehicle.getDepreciationPerKm(), distanceKm);
        BigDecimal insuranceCost = vehicle.getInsurancePerDay() != null
                ? vehicle.getInsurancePerDay().multiply(BigDecimal.valueOf(drivingDays)).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Return trip
        BigDecimal returnFuelCost = BigDecimal.ZERO;
        if (request.includeReturnTrip()) {
            BigDecimal emptyConsumption = vehicle.getConsumptionEmpty() != null
                    ? vehicle.getConsumptionEmpty() : vehicle.getConsumptionLoaded();
            returnFuelCost = distanceKm
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
                    .multiply(emptyConsumption)
                    .multiply(fuelPrice.getPricePerLiter())
                    .setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal totalCost = fuelCost.add(tollCost).add(driverCost)
                .add(maintenanceCost).add(tireCost).add(depreciationCost)
                .add(insuranceCost).add(returnFuelCost);

        BigDecimal costPerKm = distanceKm.compareTo(BigDecimal.ZERO) != 0
                ? totalCost.divide(distanceKm, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 3. Price range: price = cost / (1 - margin)
        BigDecimal minPrice = priceAtMargin(totalCost, new BigDecimal("0.15"));
        BigDecimal optimalPrice = priceAtMargin(totalCost, new BigDecimal("0.25"));
        BigDecimal premiumPrice = priceAtMargin(totalCost, new BigDecimal("0.35"));

        SuggestedPriceRange priceRange = new SuggestedPriceRange(
                totalCost, minPrice, optimalPrice, premiumPrice, distanceKm, costPerKm,
                revenuePerKm(minPrice, distanceKm),
                revenuePerKm(optimalPrice, distanceKm),
                revenuePerKm(premiumPrice, distanceKm)
        );

        // 4. Lane intelligence
        LaneIntelligence laneIntelligence = buildLaneIntelligence(
                request.originAddress(), request.destinationAddress(), distanceKm, companyId);

        // 5. Market context
        MarketContext marketContext = buildMarketContext(laneIntelligence, companyId);

        return new PricingSuggestionResponse(priceRange, laneIntelligence, marketContext);
    }

    private LaneIntelligence buildLaneIntelligence(String origin, String destination,
                                                     BigDecimal distanceKm, UUID companyId) {
        String originKey = extractCityKey(origin);
        String destKey = extractCityKey(destination);

        // Try exact match first
        List<Calculation> matches = calculationRepository.findByLane(companyId, originKey, destKey);
        String matchType = "EXACT";

        // Fallback: country pair + distance range
        if (matches.isEmpty()) {
            String originCountry = extractCountryKey(origin);
            String destCountry = extractCountryKey(destination);
            if (!originCountry.isEmpty() && !destCountry.isEmpty()) {
                BigDecimal minDist = distanceKm.multiply(new BigDecimal("0.8"));
                BigDecimal maxDist = distanceKm.multiply(new BigDecimal("1.2"));
                matches = calculationRepository.findByCountryPairAndDistance(
                        companyId, originCountry, destCountry, minDist, maxDist);
                matchType = "FUZZY";
            }
        }

        if (matches.isEmpty()) {
            return null;
        }

        BigDecimal avgPrice = avg(matches.stream().map(Calculation::getOrderPrice).toList());
        BigDecimal avgMargin = avg(matches.stream().map(Calculation::getProfitMarginPct).toList());
        BigDecimal avgRevPerKm = avg(matches.stream().map(Calculation::getRevenuePerKm).toList());

        BigDecimal bestMargin = matches.stream()
                .map(Calculation::getProfitMarginPct)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);
        BigDecimal worstMargin = matches.stream()
                .map(Calculation::getProfitMarginPct)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);

        // Monthly breakdown
        Map<YearMonth, List<Calculation>> byMonth = matches.stream()
                .filter(c -> c.getCreatedAt() != null)
                .collect(Collectors.groupingBy(c -> YearMonth.from(c.getCreatedAt()),
                        TreeMap::new, Collectors.toList()));

        List<MonthlyLaneStat> monthlyBreakdown = byMonth.entrySet().stream()
                .map(e -> new MonthlyLaneStat(
                        e.getKey().toString(),
                        e.getValue().size(),
                        avg(e.getValue().stream().map(Calculation::getOrderPrice).toList()),
                        avg(e.getValue().stream().map(Calculation::getProfitMarginPct).toList())
                ))
                .toList();

        // Trend calculation
        String trend = calculateTrend(monthlyBreakdown);

        return new LaneIntelligence(
                matches.size(), avgPrice, avgMargin, avgRevPerKm,
                trend, bestMargin, worstMargin, monthlyBreakdown, matchType
        );
    }

    private MarketContext buildMarketContext(LaneIntelligence laneIntel, UUID companyId) {
        List<Calculation> allCalcs = calculationRepository.findByCompanyId(companyId);
        if (allCalcs.isEmpty()) {
            return new MarketContext(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "UNKNOWN");
        }

        BigDecimal companyAvgMargin = avg(allCalcs.stream().map(Calculation::getProfitMarginPct).toList());
        BigDecimal companyAvgRevPerKm = avg(allCalcs.stream().map(Calculation::getRevenuePerKm).toList());

        BigDecimal marginDelta = BigDecimal.ZERO;
        if (laneIntel != null && laneIntel.avgMarginPct() != null) {
            marginDelta = laneIntel.avgMarginPct().subtract(companyAvgMargin)
                    .setScale(2, RoundingMode.HALF_UP);
        }

        // Simple seasonal pattern based on current month
        int month = java.time.LocalDate.now().getMonthValue();
        String seasonal;
        if (month >= 3 && month <= 5) seasonal = "SPRING";
        else if (month >= 6 && month <= 8) seasonal = "SUMMER";
        else if (month >= 9 && month <= 11) seasonal = "AUTUMN";
        else seasonal = "WINTER";

        return new MarketContext(companyAvgMargin, companyAvgRevPerKm, marginDelta, seasonal);
    }

    private String calculateTrend(List<MonthlyLaneStat> monthly) {
        if (monthly.size() < 2) return "STABLE";
        int half = monthly.size() / 2;
        BigDecimal firstHalfAvg = avg(monthly.subList(0, half).stream()
                .map(MonthlyLaneStat::avgPrice).toList());
        BigDecimal secondHalfAvg = avg(monthly.subList(half, monthly.size()).stream()
                .map(MonthlyLaneStat::avgPrice).toList());
        if (firstHalfAvg.compareTo(BigDecimal.ZERO) == 0) return "STABLE";
        BigDecimal change = secondHalfAvg.subtract(firstHalfAvg)
                .divide(firstHalfAvg, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        if (change.compareTo(new BigDecimal("3")) > 0) return "RISING";
        if (change.compareTo(new BigDecimal("-3")) < 0) return "FALLING";
        return "STABLE";
    }

    static String extractCityKey(String address) {
        if (address == null || address.isBlank()) return "";
        // Take first part before comma, trim, lowercase
        String[] parts = address.split(",");
        return parts[0].trim().toLowerCase();
    }

    static String extractCountryKey(String address) {
        if (address == null || address.isBlank()) return "";
        String[] parts = address.split(",");
        if (parts.length < 2) return "";
        return parts[parts.length - 1].trim().toLowerCase();
    }

    private BigDecimal priceAtMargin(BigDecimal cost, BigDecimal marginPct) {
        return cost.divide(BigDecimal.ONE.subtract(marginPct), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal revenuePerKm(BigDecimal price, BigDecimal distanceKm) {
        if (distanceKm.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return price.divide(distanceKm, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal safePerKm(BigDecimal perKm, BigDecimal distanceKm) {
        if (perKm == null) return BigDecimal.ZERO;
        return distanceKm.multiply(perKm).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal avg(List<BigDecimal> values) {
        List<BigDecimal> nonNull = values.stream().filter(Objects::nonNull).toList();
        if (nonNull.isEmpty()) return BigDecimal.ZERO;
        BigDecimal sum = nonNull.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(nonNull.size()), 2, RoundingMode.HALF_UP);
    }
}
