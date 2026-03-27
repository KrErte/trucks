package eu.fuelfleet.scraper.controller;

import eu.fuelfleet.auth.security.UserPrincipal;
import eu.fuelfleet.company.entity.Company;
import eu.fuelfleet.company.repository.CompanyRepository;
import eu.fuelfleet.scraper.dto.ScrapedTruckData;
import eu.fuelfleet.scraper.service.Truck1ScraperService;
import eu.fuelfleet.subscription.service.SubscriptionGuard;
import eu.fuelfleet.vehicle.dto.VehicleResponse;
import eu.fuelfleet.vehicle.entity.FuelType;
import eu.fuelfleet.vehicle.entity.Vehicle;
import eu.fuelfleet.vehicle.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/scraper/truck1")
@RequiredArgsConstructor
public class ScraperController {

    private final Truck1ScraperService scraperService;
    private final VehicleService vehicleService;
    private final CompanyRepository companyRepository;
    private final SubscriptionGuard subscriptionGuard;

    @GetMapping("/search")
    public List<ScrapedTruckData> searchTrucks(
            @RequestParam(defaultValue = "scania") String brand,
            @RequestParam(defaultValue = "tractor-units") String type,
            @RequestParam(defaultValue = "1") int page) {
        return scraperService.searchTrucks(brand, type, page);
    }

    @GetMapping("/details")
    public ScrapedTruckData getTruckDetails(@RequestParam String url) {
        return scraperService.getTruckDetails(url);
    }

    @PostMapping("/import")
    public ResponseEntity<VehicleResponse> importTruck(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody ScrapedTruckData truckData) {

        if (!subscriptionGuard.canAddVehicle(principal.getCompanyId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Vehicle limit reached for your subscription plan. Please upgrade.");
        }

        Company company = companyRepository.findById(principal.getCompanyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found"));

        String fuelType = truckData.getFuelType() != null ? truckData.getFuelType() : "DIESEL";

        Vehicle vehicle = Vehicle.builder()
                .company(company)
                .name(truckData.getName() != null ? truckData.getName() : truckData.getBrand() + " " + truckData.getModel())
                .fuelType(FuelType.valueOf(fuelType))
                .tankCapacity(truckData.getTankCapacity())
                .euroClass(truckData.getEuroClass())
                .axleConfiguration(truckData.getAxleConfiguration())
                .numberOfAxles(truckData.getNumberOfAxles())
                .grossWeight(truckData.getGrossWeight())
                .netWeight(truckData.getNetWeight())
                .powerHp(truckData.getPowerHp())
                .displacementCc(truckData.getDisplacementCc())
                .gearbox(truckData.getGearbox())
                .suspension(truckData.getSuspension())
                .source("TRUCK1")
                .sourceId(truckData.getSourceId())
                .sourceUrl(truckData.getSourceUrl())
                .build();

        Vehicle saved = vehicleService.createVehicle(principal.getCompanyId(), vehicle);
        return ResponseEntity.status(HttpStatus.CREATED).body(VehicleResponse.fromEntity(saved));
    }
}
