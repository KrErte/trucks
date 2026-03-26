package eu.fuelfleet.tollcost.controller;

import eu.fuelfleet.tollcost.dto.TollRateRequest;
import eu.fuelfleet.tollcost.dto.TollRateResponse;
import eu.fuelfleet.tollcost.entity.TollRate;
import eu.fuelfleet.tollcost.repository.TollRateRepository;
import eu.fuelfleet.tollcost.service.TollRateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class TollRateController {

    private final TollRateService tollRateService;
    private final TollRateRepository tollRateRepository;

    @GetMapping("/api/admin/toll-rates")
    @PreAuthorize("hasRole('ADMIN')")
    public List<TollRateResponse> getAllRates() {
        return tollRateService.getAllRates()
                .stream()
                .map(TollRateResponse::fromEntity)
                .toList();
    }

    @PostMapping("/api/admin/toll-rates")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TollRateResponse> createRate(@Valid @RequestBody TollRateRequest request) {
        TollRate rate = TollRate.builder()
                .countryCode(request.countryCode())
                .vehicleClass(request.vehicleClass())
                .costPerKm(request.costPerKm())
                .currency(request.currency() != null ? request.currency() : "EUR")
                .validFrom(LocalDate.now())
                .build();

        TollRate saved = tollRateRepository.save(rate);
        return ResponseEntity.status(HttpStatus.CREATED).body(TollRateResponse.fromEntity(saved));
    }
}
