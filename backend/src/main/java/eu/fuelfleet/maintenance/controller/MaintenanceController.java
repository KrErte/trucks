package eu.fuelfleet.maintenance.controller;

import eu.fuelfleet.maintenance.dto.MaintenanceRequest;
import eu.fuelfleet.maintenance.dto.MaintenanceResponse;
import eu.fuelfleet.maintenance.service.MaintenanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/maintenance")
@RequiredArgsConstructor
public class MaintenanceController {
    private final MaintenanceService maintenanceService;

    @GetMapping
    public List<MaintenanceResponse> getAll() { return maintenanceService.getAll(); }

    @GetMapping("/vehicle/{vehicleId}")
    public List<MaintenanceResponse> getByVehicle(@PathVariable UUID vehicleId) { return maintenanceService.getByVehicle(vehicleId); }

    @PostMapping
    public MaintenanceResponse create(@RequestBody MaintenanceRequest request) { return maintenanceService.create(request); }

    @PutMapping("/{id}")
    public MaintenanceResponse update(@PathVariable UUID id, @RequestBody MaintenanceRequest request) { return maintenanceService.update(id, request); }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) { maintenanceService.delete(id); return ResponseEntity.noContent().build(); }
}
