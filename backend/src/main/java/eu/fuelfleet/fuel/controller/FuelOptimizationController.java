package eu.fuelfleet.fuel.controller;

import eu.fuelfleet.auth.security.UserPrincipal;
import eu.fuelfleet.fuel.dto.FuelOptimizationRequest;
import eu.fuelfleet.fuel.dto.FuelOptimizationResponse;
import eu.fuelfleet.fuel.service.FuelStopOptimizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fuel-optimization")
@RequiredArgsConstructor
public class FuelOptimizationController {

    private final FuelStopOptimizationService fuelStopOptimizationService;

    @PostMapping
    public ResponseEntity<FuelOptimizationResponse> optimize(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody FuelOptimizationRequest request) {
        FuelOptimizationResponse response = fuelStopOptimizationService.optimize(request, principal.getCompanyId());
        return ResponseEntity.ok(response);
    }
}
