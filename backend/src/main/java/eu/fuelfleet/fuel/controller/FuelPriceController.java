package eu.fuelfleet.fuel.controller;

import eu.fuelfleet.fuel.dto.FuelPriceRequest;
import eu.fuelfleet.fuel.dto.FuelPriceResponse;
import eu.fuelfleet.fuel.entity.FuelPrice;
import eu.fuelfleet.fuel.repository.FuelPriceRepository;
import eu.fuelfleet.fuel.service.FuelPriceScheduler;
import eu.fuelfleet.fuel.service.FuelPriceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class FuelPriceController {

    private final FuelPriceService fuelPriceService;
    private final FuelPriceRepository fuelPriceRepository;
    private final FuelPriceScheduler fuelPriceScheduler;

    @GetMapping("/api/fuel-prices")
    public List<FuelPriceResponse> getAllCurrentPrices() {
        return fuelPriceRepository.findAll()
                .stream()
                .map(FuelPriceResponse::fromEntity)
                .toList();
    }

    @GetMapping("/api/fuel-prices/{country}")
    public List<FuelPriceResponse> getPricesByCountry(@PathVariable String country) {
        return fuelPriceService.getPricesByCountry(country)
                .stream()
                .map(FuelPriceResponse::fromEntity)
                .toList();
    }

    @GetMapping("/api/admin/fuel-prices")
    @PreAuthorize("hasRole('ADMIN')")
    public List<FuelPriceResponse> getAllPricesAdmin() {
        return fuelPriceRepository.findAll()
                .stream()
                .map(FuelPriceResponse::fromEntity)
                .toList();
    }

    @PostMapping("/api/admin/fuel-prices")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FuelPriceResponse> createOrUpdatePrice(@Valid @RequestBody FuelPriceRequest request) {
        FuelPrice price = FuelPrice.builder()
                .countryCode(request.countryCode())
                .fuelType(request.fuelType())
                .pricePerLiter(request.pricePerLiter())
                .validFrom(LocalDate.now())
                .source(request.source())
                .build();

        FuelPrice saved = fuelPriceService.updatePrice(price);
        return ResponseEntity.status(HttpStatus.CREATED).body(FuelPriceResponse.fromEntity(saved));
    }

    @PostMapping("/api/fuel-prices/refresh")
    public ResponseEntity<Map<String, Object>> refreshPrices() {
        int updated = fuelPriceScheduler.refreshAll();
        return ResponseEntity.ok(Map.of(
                "message", "Fuel prices refreshed",
                "updatedCount", updated
        ));
    }
}
