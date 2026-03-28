package eu.fuelfleet.driver.controller;

import eu.fuelfleet.driver.dto.DriverRequest;
import eu.fuelfleet.driver.dto.DriverResponse;
import eu.fuelfleet.driver.service.DriverService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
public class DriverController {
    private final DriverService driverService;

    @GetMapping
    public List<DriverResponse> getAll() {
        return driverService.getAll();
    }

    @GetMapping("/active")
    public List<DriverResponse> getActive() {
        return driverService.getActive();
    }

    @PostMapping
    public DriverResponse create(@RequestBody DriverRequest request) {
        return driverService.create(request);
    }

    @PutMapping("/{id}")
    public DriverResponse update(@PathVariable UUID id, @RequestBody DriverRequest request) {
        return driverService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        driverService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        driverService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activate(@PathVariable UUID id) {
        driverService.activate(id);
        return ResponseEntity.noContent().build();
    }
}
