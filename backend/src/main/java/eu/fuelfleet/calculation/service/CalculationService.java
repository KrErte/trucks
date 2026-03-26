package eu.fuelfleet.calculation.service;

import eu.fuelfleet.auth.entity.User;
import eu.fuelfleet.auth.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.fuelfleet.calculation.dto.CalculateRequest;
import eu.fuelfleet.calculation.dto.CalculationResponse;
import eu.fuelfleet.calculation.dto.DistanceResult;
import eu.fuelfleet.calculation.dto.LegDetail;
import eu.fuelfleet.calculation.dto.RouteAlternative;
import eu.fuelfleet.calculation.entity.Calculation;
import eu.fuelfleet.calculation.repository.CalculationRepository;
import eu.fuelfleet.company.entity.Company;
import eu.fuelfleet.company.service.CompanyService;
import eu.fuelfleet.fuel.entity.FuelPrice;
import eu.fuelfleet.fuel.service.FuelPriceService;
import eu.fuelfleet.tollcost.entity.TollRate;
import eu.fuelfleet.tollcost.repository.TollRateRepository;
import eu.fuelfleet.tollcost.service.TollRateService;
import eu.fuelfleet.vehicle.entity.FuelType;
import eu.fuelfleet.vehicle.entity.Vehicle;
import eu.fuelfleet.vehicle.service.VehicleService;
import eu.fuelfleet.webhook.service.WebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import eu.fuelfleet.calculation.dto.TollBreakdown;

@Service
@RequiredArgsConstructor
public class CalculationService {

    private final CalculationRepository calculationRepository;
    private final VehicleService vehicleService;
    private final FuelPriceService fuelPriceService;
    private final TollRateService tollRateService;
    private final TollRateRepository tollRateRepository;
    private final GoogleMapsService googleMapsService;
    private final CompanyService companyService;
    private final UserRepository userRepository;
    private final WebhookService webhookService;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public Calculation calculate(CalculateRequest request, UUID companyId, UUID userId) {
        Vehicle vehicle = vehicleService.getVehicle(request.vehicleId(), companyId);
        Company company = companyService.getCompany(companyId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Build address list: origin + waypoints + destination
        List<String> addresses = new ArrayList<>();
        addresses.add(request.originAddress());
        if (request.waypoints() != null) {
            addresses.addAll(request.waypoints());
        }
        addresses.add(request.destinationAddress());

        // Calculate distances for each leg
        List<DistanceResult> legDistances = googleMapsService.getMultiLegDistances(addresses);

        // Build leg details and sum totals
        List<LegDetail> legDetails = new ArrayList<>();
        BigDecimal sumDistanceKm = BigDecimal.ZERO;
        BigDecimal sumEstimatedHours = BigDecimal.ZERO;
        for (int i = 0; i < legDistances.size(); i++) {
            DistanceResult leg = legDistances.get(i);
            legDetails.add(new LegDetail(addresses.get(i), addresses.get(i + 1), leg.distanceKm(), leg.estimatedHours()));
            sumDistanceKm = sumDistanceKm.add(leg.distanceKm());
            sumEstimatedHours = sumEstimatedHours.add(leg.estimatedHours());
        }
        final BigDecimal totalDistanceKm = sumDistanceKm;
        final BigDecimal totalEstimatedHours = sumEstimatedHours;

        // Fuel cost - use company's country for pricing
        String countryCode = (company.getCountry() != null && !company.getCountry().isBlank()) ? company.getCountry() : "EE";
        FuelPrice fuelPrice = fuelPriceService.getCurrentPrice(
                countryCode, vehicle.getFuelType().name());

        // Use loaded or empty consumption based on isLoaded flag
        BigDecimal consumption = request.isLoaded()
                ? vehicle.getConsumptionLoaded()
                : (vehicle.getConsumptionEmpty() != null ? vehicle.getConsumptionEmpty() : vehicle.getConsumptionLoaded());

        BigDecimal fuelCost = totalDistanceKm
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
                .multiply(consumption)
                .multiply(fuelPrice.getPricePerLiter())
                .setScale(2, RoundingMode.HALF_UP);

        // Toll cost - country-based breakdown
        Map<String, BigDecimal> countryDistances = googleMapsService.getCountryDistances(
                request.originAddress(), request.destinationAddress());
        List<TollBreakdown> tollBreakdownList = new ArrayList<>();
        BigDecimal tollCost = BigDecimal.ZERO;

        if (!countryDistances.isEmpty()) {
            for (Map.Entry<String, BigDecimal> entry : countryDistances.entrySet()) {
                String cc = entry.getKey();
                BigDecimal dist = entry.getValue();
                Optional<TollRate> rateOpt = tollRateRepository.findCurrentRate(cc, vehicle.getEuroClass());
                if (rateOpt.isPresent()) {
                    BigDecimal costPerKm = rateOpt.get().getCostPerKm();
                    BigDecimal cost = dist.multiply(costPerKm).setScale(2, RoundingMode.HALF_UP);
                    tollCost = tollCost.add(cost);
                    tollBreakdownList.add(new TollBreakdown(cc, dist, costPerKm, cost));
                } else {
                    tollBreakdownList.add(new TollBreakdown(cc, dist, BigDecimal.ZERO, BigDecimal.ZERO));
                }
            }
        } else {
            // Fallback to single-country calculation
            tollCost = tollRateRepository.findCurrentRate(countryCode, vehicle.getEuroClass())
                    .map(tollRate -> totalDistanceKm.multiply(tollRate.getCostPerKm()).setScale(2, RoundingMode.HALF_UP))
                    .orElse(BigDecimal.ZERO);
        }

        // Driver cost
        BigDecimal driverDailyRate = request.driverDailyRate() != null
                ? request.driverDailyRate()
                : company.getDefaultDriverDailyRate();
        if (driverDailyRate == null) {
            driverDailyRate = BigDecimal.ZERO;
        }
        long drivingDays = totalEstimatedHours
                .divide(BigDecimal.valueOf(24), 0, RoundingMode.CEILING)
                .longValue();
        if (drivingDays < 1) drivingDays = 1;
        BigDecimal driverCost = driverDailyRate.multiply(BigDecimal.valueOf(drivingDays))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal otherCosts = request.otherCosts() != null ? request.otherCosts() : BigDecimal.ZERO;

        // Maintenance, tire, depreciation costs (per km)
        BigDecimal maintenanceCost = vehicle.getMaintenanceCostPerKm() != null
                ? totalDistanceKm.multiply(vehicle.getMaintenanceCostPerKm()).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal tireCost = vehicle.getTireCostPerKm() != null
                ? totalDistanceKm.multiply(vehicle.getTireCostPerKm()).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal depreciationCost = vehicle.getDepreciationPerKm() != null
                ? totalDistanceKm.multiply(vehicle.getDepreciationPerKm()).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Insurance cost (per day)
        BigDecimal insuranceCost = vehicle.getInsurancePerDay() != null
                ? vehicle.getInsurancePerDay().multiply(BigDecimal.valueOf(drivingDays)).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Return trip fuel cost (empty truck returning)
        BigDecimal returnFuelCost = BigDecimal.ZERO;
        if (request.includeReturnTrip()) {
            BigDecimal emptyConsumption = vehicle.getConsumptionEmpty() != null
                    ? vehicle.getConsumptionEmpty() : vehicle.getConsumptionLoaded();
            returnFuelCost = totalDistanceKm
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
                    .multiply(emptyConsumption)
                    .multiply(fuelPrice.getPricePerLiter())
                    .setScale(2, RoundingMode.HALF_UP);
        }

        // CO2 emissions calculation
        BigDecimal totalFuelLiters = totalDistanceKm
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
                .multiply(consumption);
        BigDecimal co2EmissionsKg = calculateCo2(vehicle.getFuelType(), totalFuelLiters);
        if (request.includeReturnTrip()) {
            BigDecimal returnFuelLiters = totalDistanceKm
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
                    .multiply(vehicle.getConsumptionEmpty() != null ? vehicle.getConsumptionEmpty() : vehicle.getConsumptionLoaded());
            co2EmissionsKg = co2EmissionsKg.add(calculateCo2(vehicle.getFuelType(), returnFuelLiters));
        }

        BigDecimal totalCost = fuelCost.add(tollCost).add(driverCost).add(otherCosts).add(returnFuelCost)
                .add(maintenanceCost).add(tireCost).add(depreciationCost).add(insuranceCost);
        BigDecimal costPerKm = totalDistanceKm.compareTo(BigDecimal.ZERO) != 0
                ? totalCost.divide(totalDistanceKm, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal profit = request.orderPrice().subtract(totalCost);
        BigDecimal profitMarginPct = request.orderPrice().compareTo(BigDecimal.ZERO) != 0
                ? profit.divide(request.orderPrice(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal revenuePerKm = totalDistanceKm.compareTo(BigDecimal.ZERO) != 0
                ? request.orderPrice().divide(totalDistanceKm, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Serialize waypoints, leg details, and toll breakdown
        String waypointsJson = null;
        String legsJson = null;
        String tollBreakdownJson = null;
        try {
            if (request.waypoints() != null && !request.waypoints().isEmpty()) {
                waypointsJson = objectMapper.writeValueAsString(request.waypoints());
            }
            legsJson = objectMapper.writeValueAsString(legDetails);
            if (!tollBreakdownList.isEmpty()) {
                tollBreakdownJson = objectMapper.writeValueAsString(tollBreakdownList);
            }
        } catch (Exception ignored) {}

        Calculation calculation = Calculation.builder()
                .company(company)
                .vehicle(vehicle)
                .user(user)
                .origin(request.originAddress())
                .destination(request.destinationAddress())
                .waypoints(waypointsJson)
                .distanceKm(totalDistanceKm)
                .estimatedHours(totalEstimatedHours)
                .cargoWeightT(request.cargoWeightTons())
                .orderPrice(request.orderPrice())
                .currency(request.currency())
                .fuelCost(fuelCost)
                .tollCost(tollCost)
                .driverDailyCost(driverCost)
                .otherCosts(otherCosts)
                .totalCost(totalCost)
                .profit(profit)
                .profitMarginPct(profitMarginPct)
                .revenuePerKm(revenuePerKm)
                .includeReturnTrip(request.includeReturnTrip())
                .returnFuelCost(returnFuelCost)
                .fuelBreakdown(legsJson)
                .tollBreakdown(tollBreakdownJson)
                .co2EmissionsKg(co2EmissionsKg.setScale(2, RoundingMode.HALF_UP))
                .driverDays((int) drivingDays)
                .costPerKm(costPerKm.setScale(4, RoundingMode.HALF_UP))
                .maintenanceCost(maintenanceCost)
                .tireCost(tireCost)
                .depreciationCost(depreciationCost)
                .insuranceCost(insuranceCost)
                .build();

        Calculation saved = calculationRepository.save(calculation);

        // Dispatch webhook
        webhookService.dispatch(companyId, "calculation.created", CalculationResponse.fromEntity(saved));

        return saved;
    }

    /**
     * Preview alternative routes without saving.
     */
    public List<RouteAlternative> previewAlternatives(CalculateRequest request, UUID companyId) {
        Vehicle vehicle = vehicleService.getVehicle(request.vehicleId(), companyId);
        Company company = companyService.getCompany(companyId);

        List<DistanceResult> altRoutes = googleMapsService.getAlternativeRoutes(
                request.originAddress(), request.destinationAddress());

        String countryCode = (company.getCountry() != null && !company.getCountry().isBlank()) ? company.getCountry() : "EE";
        FuelPrice fuelPrice = fuelPriceService.getCurrentPrice(countryCode, vehicle.getFuelType().name());
        BigDecimal consumption = request.isLoaded()
                ? vehicle.getConsumptionLoaded()
                : (vehicle.getConsumptionEmpty() != null ? vehicle.getConsumptionEmpty() : vehicle.getConsumptionLoaded());

        String[] labels = {"Kiireim", "Alternatiiv 1", "Alternatiiv 2"};

        List<RouteAlternative> alternatives = new java.util.ArrayList<>();
        for (int i = 0; i < altRoutes.size(); i++) {
            DistanceResult route = altRoutes.get(i);
            BigDecimal fuelCost = route.distanceKm()
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
                    .multiply(consumption)
                    .multiply(fuelPrice.getPricePerLiter())
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal tollCost = tollRateRepository.findCurrentRate(countryCode, vehicle.getEuroClass())
                    .map(tr -> route.distanceKm().multiply(tr.getCostPerKm()).setScale(2, RoundingMode.HALF_UP))
                    .orElse(BigDecimal.ZERO);

            BigDecimal estimatedTotal = fuelCost.add(tollCost);

            String label = i < labels.length ? labels[i] : "Alternatiiv " + i;
            alternatives.add(new RouteAlternative(
                    i, label, route.distanceKm(), route.estimatedHours(),
                    route.summary() != null ? route.summary() : "",
                    fuelCost, estimatedTotal
            ));
        }
        return alternatives;
    }

    @Transactional(readOnly = true)
    public Page<Calculation> getHistory(UUID companyId, Pageable pageable) {
        return calculationRepository.findByCompanyIdOrderByCreatedAtDesc(companyId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<CalculationResponse> getHistoryResponse(UUID companyId, Pageable pageable) {
        return calculationRepository.findByCompanyIdOrderByCreatedAtDesc(companyId, pageable)
                .map(CalculationResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<Calculation> getCalculationsSince(UUID companyId, java.time.LocalDateTime since) {
        return calculationRepository.findByCompanyIdAndCreatedAtAfter(companyId, since);
    }

    private BigDecimal calculateCo2(FuelType fuelType, BigDecimal fuelLiters) {
        BigDecimal factor = switch (fuelType) {
            case DIESEL -> new BigDecimal("2.64");
            case LNG -> new BigDecimal("1.18");
            case CNG -> new BigDecimal("2.75");
            case ELECTRIC -> BigDecimal.ZERO;
        };
        return fuelLiters.multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public Calculation getCalculation(UUID id, UUID companyId) {
        Calculation calculation = calculationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Calculation not found"));
        if (!calculation.getCompany().getId().equals(companyId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Calculation does not belong to your company");
        }
        return calculation;
    }
}
