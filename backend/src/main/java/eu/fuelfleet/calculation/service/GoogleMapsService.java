package eu.fuelfleet.calculation.service;

import com.fasterxml.jackson.databind.JsonNode;
import eu.fuelfleet.calculation.dto.DistanceResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.util.*;

@Service
public class GoogleMapsService {

    private final RestTemplate restTemplate;
    private final String apiKey;

    private static final String DISTANCE_MATRIX_URL = "https://maps.googleapis.com/maps/api/distancematrix/json";
    private static final String DIRECTIONS_URL = "https://maps.googleapis.com/maps/api/directions/json";
    private static final String PLACES_AUTOCOMPLETE_URL = "https://maps.googleapis.com/maps/api/place/autocomplete/json";
    private static final String GEOCODE_URL = "https://maps.googleapis.com/maps/api/geocode/json";

    public GoogleMapsService(
            RestTemplate restTemplate,
            @Value("${app.google-maps.api-key:}") String apiKey) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
    }

    public DistanceResult getDistanceAndDuration(String origin, String destination) {
        URI uri = UriComponentsBuilder.fromHttpUrl(DISTANCE_MATRIX_URL)
                .queryParam("origins", origin)
                .queryParam("destinations", destination)
                .queryParam("key", apiKey)
                .queryParam("units", "metric")
                .build()
                .encode()
                .toUri();

        JsonNode response = restTemplate.getForObject(uri, JsonNode.class);

        if (response == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No response from Google Maps API");
        }

        String status = response.path("status").asText();
        if (!"OK".equals(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Google Maps API error: " + status);
        }

        JsonNode element = response.path("rows").path(0).path("elements").path(0);
        String elementStatus = element.path("status").asText();
        if (!"OK".equals(elementStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Route not found: " + elementStatus);
        }

        long distanceMeters = element.path("distance").path("value").asLong();
        long durationSeconds = element.path("duration").path("value").asLong();

        BigDecimal distanceKm = BigDecimal.valueOf(distanceMeters)
                .divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP);
        BigDecimal estimatedHours = BigDecimal.valueOf(durationSeconds)
                .divide(BigDecimal.valueOf(3600), 2, RoundingMode.HALF_UP);

        return new DistanceResult(distanceKm, estimatedHours);
    }

    /**
     * Get alternative routes using the Directions API.
     * Returns up to 3 alternative routes.
     */
    public List<DistanceResult> getAlternativeRoutes(String origin, String destination) {
        URI uri = UriComponentsBuilder.fromHttpUrl(DIRECTIONS_URL)
                .queryParam("origin", origin)
                .queryParam("destination", destination)
                .queryParam("key", apiKey)
                .queryParam("alternatives", "true")
                .build()
                .encode()
                .toUri();

        JsonNode response = restTemplate.getForObject(uri, JsonNode.class);

        if (response == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No response from Google Maps Directions API");
        }

        String status = response.path("status").asText();
        if (!"OK".equals(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Google Maps Directions API error: " + status);
        }

        List<DistanceResult> results = new ArrayList<>();
        JsonNode routes = response.path("routes");
        for (int i = 0; i < routes.size() && i < 3; i++) {
            JsonNode route = routes.get(i);
            JsonNode leg = route.path("legs").path(0);
            long distanceMeters = leg.path("distance").path("value").asLong();
            long durationSeconds = leg.path("duration").path("value").asLong();
            String summary = route.path("summary").asText("");

            BigDecimal distanceKm = BigDecimal.valueOf(distanceMeters)
                    .divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP);
            BigDecimal estimatedHours = BigDecimal.valueOf(durationSeconds)
                    .divide(BigDecimal.valueOf(3600), 2, RoundingMode.HALF_UP);

            results.add(new DistanceResult(distanceKm, estimatedHours, summary));
        }

        return results;
    }

    /**
     * Calculate distances for each consecutive pair in the address list.
     * Returns one DistanceResult per leg (addresses.size() - 1 results).
     */
    public List<DistanceResult> getMultiLegDistances(List<String> addresses) {
        List<DistanceResult> results = new ArrayList<>();
        for (int i = 0; i < addresses.size() - 1; i++) {
            results.add(getDistanceAndDuration(addresses.get(i), addresses.get(i + 1)));
        }
        return results;
    }

    /**
     * Google Places Autocomplete proxy.
     */
    public List<Map<String, String>> autocomplete(String input) {
        URI uri = UriComponentsBuilder.fromHttpUrl(PLACES_AUTOCOMPLETE_URL)
                .queryParam("input", input)
                .queryParam("key", apiKey)
                .queryParam("types", "geocode|establishment")
                .build()
                .encode()
                .toUri();

        JsonNode response = restTemplate.getForObject(uri, JsonNode.class);
        if (response == null || !"OK".equals(response.path("status").asText()) && !"ZERO_RESULTS".equals(response.path("status").asText())) {
            return List.of();
        }

        List<Map<String, String>> results = new ArrayList<>();
        JsonNode predictions = response.path("predictions");
        for (JsonNode p : predictions) {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("description", p.path("description").asText());
            item.put("placeId", p.path("place_id").asText());
            results.add(item);
        }
        return results;
    }

    /**
     * Get distance breakdown by country for a route using Directions API.
     * Uses reverse geocoding of step endpoints to determine country borders.
     * Returns a map of country code -> distance in km.
     */
    public Map<String, BigDecimal> getCountryDistances(String origin, String destination) {
        URI uri = UriComponentsBuilder.fromHttpUrl(DIRECTIONS_URL)
                .queryParam("origin", origin)
                .queryParam("destination", destination)
                .queryParam("key", apiKey)
                .build()
                .encode()
                .toUri();

        JsonNode response = restTemplate.getForObject(uri, JsonNode.class);
        if (response == null || !"OK".equals(response.path("status").asText())) {
            return Map.of();
        }

        Map<String, BigDecimal> countryDistances = new LinkedHashMap<>();
        JsonNode routes = response.path("routes");
        if (routes.isEmpty()) return countryDistances;

        JsonNode legs = routes.get(0).path("legs");
        for (JsonNode leg : legs) {
            JsonNode steps = leg.path("steps");
            for (JsonNode step : steps) {
                long stepMeters = step.path("distance").path("value").asLong();
                BigDecimal stepKm = BigDecimal.valueOf(stepMeters)
                        .divide(BigDecimal.valueOf(1000), 4, RoundingMode.HALF_UP);

                // Use end_location of each step for geocoding
                double lat = step.path("end_location").path("lat").asDouble();
                double lng = step.path("end_location").path("lng").asDouble();
                String country = reverseGeocodeCountry(lat, lng);
                if (country == null) country = "XX";

                countryDistances.merge(country, stepKm, BigDecimal::add);
            }
        }

        // Round final values
        countryDistances.replaceAll((k, v) -> v.setScale(2, RoundingMode.HALF_UP));
        return countryDistances;
    }

    /**
     * Reverse geocode a lat/lng to get the country code.
     */
    private String reverseGeocodeCountry(double lat, double lng) {
        URI uri = UriComponentsBuilder.fromHttpUrl(GEOCODE_URL)
                .queryParam("latlng", lat + "," + lng)
                .queryParam("key", apiKey)
                .queryParam("result_type", "country")
                .build()
                .encode()
                .toUri();

        try {
            JsonNode response = restTemplate.getForObject(uri, JsonNode.class);
            if (response == null || !"OK".equals(response.path("status").asText())) {
                return null;
            }
            JsonNode results = response.path("results");
            if (results.isEmpty()) return null;

            JsonNode components = results.get(0).path("address_components");
            for (JsonNode comp : components) {
                JsonNode types = comp.path("types");
                for (JsonNode type : types) {
                    if ("country".equals(type.asText())) {
                        return comp.path("short_name").asText();
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
}
