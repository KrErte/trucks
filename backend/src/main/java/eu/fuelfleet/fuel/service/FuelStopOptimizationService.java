package eu.fuelfleet.fuel.service;

import eu.fuelfleet.fuel.dto.FuelOptimizationRequest;
import eu.fuelfleet.fuel.dto.FuelOptimizationResponse;
import eu.fuelfleet.fuel.dto.FuelStopSuggestion;
import eu.fuelfleet.fuel.entity.FuelPrice;
import eu.fuelfleet.vehicle.entity.Vehicle;
import eu.fuelfleet.vehicle.service.VehicleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class FuelStopOptimizationService {

    private final FuelPriceService fuelPriceService;
    private final VehicleService vehicleService;
    private final JdbcTemplate jdbcTemplate;

    private static final Map<String, String> COUNTRY_NAMES = Map.ofEntries(
            Map.entry("EE", "Eesti"), Map.entry("LV", "Läti"), Map.entry("LT", "Leedu"),
            Map.entry("PL", "Poola"), Map.entry("DE", "Saksamaa"), Map.entry("NL", "Holland"),
            Map.entry("BE", "Belgia"), Map.entry("FR", "Prantsusmaa"), Map.entry("AT", "Austria"),
            Map.entry("CZ", "Tšehhi"), Map.entry("IT", "Itaalia"), Map.entry("ES", "Hispaania"),
            Map.entry("SE", "Rootsi"), Map.entry("FI", "Soome"), Map.entry("DK", "Taani"),
            Map.entry("NO", "Norra"), Map.entry("HU", "Ungari"), Map.entry("RO", "Rumeenia"),
            Map.entry("SK", "Slovakkia"), Map.entry("SI", "Sloveenia"), Map.entry("HR", "Horvaatia"),
            Map.entry("BG", "Bulgaaria"), Map.entry("CH", "Šveits"), Map.entry("GB", "Suurbritannia")
    );

    public FuelOptimizationResponse optimize(FuelOptimizationRequest request, UUID companyId) {
        Vehicle vehicle = vehicleService.getVehicle(request.vehicleId(), companyId);

        BigDecimal consumption = request.isLoaded()
                ? vehicle.getConsumptionLoaded()
                : (vehicle.getConsumptionEmpty() != null ? vehicle.getConsumptionEmpty() : vehicle.getConsumptionLoaded());

        BigDecimal tankCapacity = vehicle.getTankCapacity() != null ? vehicle.getTankCapacity() : BigDecimal.valueOf(500);

        // Get countries along the route from the corridor table
        List<String> countries = getRouteCountries(request.originAddress(), request.destinationAddress());
        if (countries.isEmpty()) {
            countries = List.of("EE"); // fallback
        }

        String fuelType = vehicle.getFuelType().name();

        // Get fuel prices for each country
        Map<String, BigDecimal> prices = new LinkedHashMap<>();
        for (String country : countries) {
            try {
                FuelPrice fp = fuelPriceService.getCurrentPrice(country, fuelType);
                prices.put(country, fp.getPricePerLiter());
            } catch (Exception e) {
                log.debug("No fuel price for country {} and type {}", country, fuelType);
            }
        }

        if (prices.isEmpty()) {
            return new FuelOptimizationResponse(List.of(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        // Find cheapest country
        String cheapestCountry = prices.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(countries.get(0));

        // Simulate: divide distance equally among countries
        BigDecimal totalDistance = request.distanceKm();
        BigDecimal segmentDistance = totalDistance.divide(BigDecimal.valueOf(countries.size()), 2, RoundingMode.HALF_UP);
        BigDecimal totalFuelNeeded = totalDistance.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP).multiply(consumption);
        BigDecimal currentFuel = request.currentFuelLiters() != null ? request.currentFuelLiters() : tankCapacity.multiply(BigDecimal.valueOf(0.5));
        BigDecimal fuelPerSegment = segmentDistance.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP).multiply(consumption);

        // Greedy optimization: fill up in cheap countries, minimum in expensive
        List<FuelStopSuggestion> stops = new ArrayList<>();
        BigDecimal optimizedCost = BigDecimal.ZERO;

        for (String country : countries) {
            if (!prices.containsKey(country)) continue;
            BigDecimal price = prices.get(country);

            currentFuel = currentFuel.subtract(fuelPerSegment);
            if (currentFuel.compareTo(BigDecimal.ZERO) < 0) currentFuel = BigDecimal.ZERO;

            BigDecimal litersToFill;
            String reason;

            if (country.equals(cheapestCountry)) {
                // Fill up tank fully in cheapest country
                litersToFill = tankCapacity.subtract(currentFuel).max(BigDecimal.ZERO);
                reason = "Odavaim hind - tangi täis";
            } else if (currentFuel.compareTo(fuelPerSegment.multiply(BigDecimal.valueOf(1.5))) < 0) {
                // Low fuel - fill minimum to reach next segment
                litersToFill = fuelPerSegment.multiply(BigDecimal.valueOf(2)).subtract(currentFuel).max(BigDecimal.ZERO);
                reason = "Madal kütusetase - miinimum";
            } else {
                // Expensive country, skip fueling
                continue;
            }

            if (litersToFill.compareTo(BigDecimal.valueOf(10)) < 0) continue;

            BigDecimal cost = litersToFill.multiply(price).setScale(2, RoundingMode.HALF_UP);
            currentFuel = currentFuel.add(litersToFill);
            optimizedCost = optimizedCost.add(cost);

            stops.add(new FuelStopSuggestion(
                    country,
                    COUNTRY_NAMES.getOrDefault(country, country),
                    price,
                    litersToFill.setScale(1, RoundingMode.HALF_UP),
                    cost,
                    reason
            ));
        }

        // Naive cost: fill everything at home country price
        BigDecimal homePrice = prices.values().iterator().next();
        BigDecimal naiveCost = totalFuelNeeded.multiply(homePrice).setScale(2, RoundingMode.HALF_UP);

        if (optimizedCost.compareTo(BigDecimal.ZERO) == 0) {
            optimizedCost = naiveCost;
        }

        BigDecimal savings = naiveCost.subtract(optimizedCost).max(BigDecimal.ZERO);
        BigDecimal savingsPercent = naiveCost.compareTo(BigDecimal.ZERO) > 0
                ? savings.divide(naiveCost, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new FuelOptimizationResponse(stops, optimizedCost, naiveCost, savings, savingsPercent);
    }

    private List<String> getRouteCountries(String origin, String destination) {
        // Extract country codes from address strings
        String originCountry = extractCountryCode(origin);
        String destCountry = extractCountryCode(destination);

        if (originCountry != null && destCountry != null) {
            // Try corridor table
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT waypoint_countries FROM route_country_segments WHERE origin_country = ? AND destination_country = ? LIMIT 1",
                    originCountry, destCountry
            );
            if (!rows.isEmpty()) {
                String waypoints = (String) rows.get(0).get("waypoint_countries");
                return Arrays.asList(waypoints.split(","));
            }
            // Try reverse direction
            rows = jdbcTemplate.queryForList(
                    "SELECT waypoint_countries FROM route_country_segments WHERE origin_country = ? AND destination_country = ? LIMIT 1",
                    destCountry, originCountry
            );
            if (!rows.isEmpty()) {
                String waypoints = (String) rows.get(0).get("waypoint_countries");
                List<String> reversed = new ArrayList<>(Arrays.asList(waypoints.split(",")));
                Collections.reverse(reversed);
                return reversed;
            }
            return List.of(originCountry, destCountry);
        }

        return List.of("EE");
    }

    private String extractCountryCode(String address) {
        if (address == null) return null;
        String upper = address.toUpperCase().trim();

        Map<String, String> keywords = Map.ofEntries(
                Map.entry("EESTI", "EE"), Map.entry("ESTONIA", "EE"),
                Map.entry("LÄTI", "LV"), Map.entry("LATVIA", "LV"),
                Map.entry("LEEDU", "LT"), Map.entry("LITHUANIA", "LT"),
                Map.entry("POOLA", "PL"), Map.entry("POLAND", "PL"),
                Map.entry("SAKSAMAA", "DE"), Map.entry("GERMANY", "DE"), Map.entry("DEUTSCHLAND", "DE"),
                Map.entry("HOLLAND", "NL"), Map.entry("NETHERLANDS", "NL"),
                Map.entry("BELGIA", "BE"), Map.entry("BELGIUM", "BE"),
                Map.entry("PRANTSUSMAA", "FR"), Map.entry("FRANCE", "FR"),
                Map.entry("AUSTRIA", "AT"), Map.entry("TŠEHHI", "CZ"), Map.entry("CZECH", "CZ"),
                Map.entry("ITAALIA", "IT"), Map.entry("ITALY", "IT"), Map.entry("ITALIA", "IT"),
                Map.entry("HISPAANIA", "ES"), Map.entry("SPAIN", "ES"),
                Map.entry("ROOTSI", "SE"), Map.entry("SWEDEN", "SE"),
                Map.entry("SOOME", "FI"), Map.entry("FINLAND", "FI"),
                Map.entry("TAANI", "DK"), Map.entry("DENMARK", "DK"),
                Map.entry("NORRA", "NO"), Map.entry("NORWAY", "NO"),
                Map.entry("UNGARI", "HU"), Map.entry("HUNGARY", "HU"),
                Map.entry("RUMEENIA", "RO"), Map.entry("ROMANIA", "RO"),
                Map.entry("SLOVAKKIA", "SK"), Map.entry("SLOVAKIA", "SK"),
                Map.entry("SLOVEENIA", "SI"), Map.entry("SLOVENIA", "SI"),
                Map.entry("HORVAATIA", "HR"), Map.entry("CROATIA", "HR"),
                Map.entry("BULGAARIA", "BG"), Map.entry("BULGARIA", "BG"),
                Map.entry("ŠVEITS", "CH"), Map.entry("SWITZERLAND", "CH")
        );

        for (Map.Entry<String, String> entry : keywords.entrySet()) {
            if (upper.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }
}
