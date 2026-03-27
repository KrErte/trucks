package eu.fuelfleet.vehicle.controller;

import eu.fuelfleet.auth.security.UserPrincipal;
import eu.fuelfleet.company.entity.Company;
import eu.fuelfleet.company.repository.CompanyRepository;
import eu.fuelfleet.vehicle.dto.VehicleRequest;
import eu.fuelfleet.vehicle.dto.VehicleResponse;
import eu.fuelfleet.vehicle.entity.FuelType;
import eu.fuelfleet.vehicle.entity.Vehicle;
import eu.fuelfleet.vehicle.service.VehicleService;
import eu.fuelfleet.subscription.service.SubscriptionGuard;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;
    private final CompanyRepository companyRepository;
    private final SubscriptionGuard subscriptionGuard;

    @GetMapping
    public List<VehicleResponse> listVehicles(@AuthenticationPrincipal UserPrincipal principal) {
        return vehicleService.getVehiclesByCompany(principal.getCompanyId())
                .stream()
                .map(VehicleResponse::fromEntity)
                .toList();
    }

    @PostMapping
    public ResponseEntity<VehicleResponse> createVehicle(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody VehicleRequest request) {
        if (!subscriptionGuard.canAddVehicle(principal.getCompanyId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Vehicle limit reached for your subscription plan. Please upgrade.");
        }

        Company company = companyRepository.findById(principal.getCompanyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found"));

        Vehicle vehicle = Vehicle.builder()
                .company(company)
                .name(request.name())
                .fuelType(FuelType.valueOf(request.fuelType()))
                .consumptionLoaded(request.consumptionLoaded())
                .consumptionEmpty(request.consumptionEmpty())
                .tankCapacity(request.tankCapacity())
                .euroClass(request.euroClass())
                .maintenanceCostPerKm(request.maintenanceCostPerKm())
                .tireCostPerKm(request.tireCostPerKm())
                .depreciationPerKm(request.depreciationPerKm())
                .insurancePerDay(request.insurancePerDay())
                .axleConfiguration(request.axleConfiguration())
                .numberOfAxles(request.numberOfAxles())
                .grossWeight(request.grossWeight())
                .netWeight(request.netWeight())
                .powerHp(request.powerHp())
                .displacementCc(request.displacementCc())
                .gearbox(request.gearbox())
                .suspension(request.suspension())
                .source(request.source() != null ? request.source() : "MANUAL")
                .sourceId(request.sourceId())
                .sourceUrl(request.sourceUrl())
                .build();

        Vehicle saved = vehicleService.createVehicle(principal.getCompanyId(), vehicle);
        return ResponseEntity.status(HttpStatus.CREATED).body(VehicleResponse.fromEntity(saved));
    }

    @PutMapping("/{id}")
    public VehicleResponse updateVehicle(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody VehicleRequest request) {
        Vehicle updates = Vehicle.builder()
                .name(request.name())
                .fuelType(FuelType.valueOf(request.fuelType()))
                .consumptionLoaded(request.consumptionLoaded())
                .consumptionEmpty(request.consumptionEmpty())
                .tankCapacity(request.tankCapacity())
                .euroClass(request.euroClass())
                .maintenanceCostPerKm(request.maintenanceCostPerKm())
                .tireCostPerKm(request.tireCostPerKm())
                .depreciationPerKm(request.depreciationPerKm())
                .insurancePerDay(request.insurancePerDay())
                .axleConfiguration(request.axleConfiguration())
                .numberOfAxles(request.numberOfAxles())
                .grossWeight(request.grossWeight())
                .netWeight(request.netWeight())
                .powerHp(request.powerHp())
                .displacementCc(request.displacementCc())
                .gearbox(request.gearbox())
                .suspension(request.suspension())
                .build();

        Vehicle updated = vehicleService.updateVehicle(id, principal.getCompanyId(), updates);
        return VehicleResponse.fromEntity(updated);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteVehicle(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        vehicleService.deleteVehicle(id, principal.getCompanyId());
    }
}
