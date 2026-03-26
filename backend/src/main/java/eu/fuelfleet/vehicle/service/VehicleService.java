package eu.fuelfleet.vehicle.service;

import eu.fuelfleet.vehicle.entity.Vehicle;
import eu.fuelfleet.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    @Transactional(readOnly = true)
    public List<Vehicle> getVehiclesByCompany(UUID companyId) {
        return vehicleRepository.findByCompanyIdAndActiveTrue(companyId);
    }

    @Transactional(readOnly = true)
    public Vehicle getVehicle(UUID id, UUID companyId) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehicle not found"));
        if (!vehicle.getCompany().getId().equals(companyId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vehicle does not belong to your company");
        }
        return vehicle;
    }

    @Transactional
    public Vehicle createVehicle(UUID companyId, Vehicle vehicle) {
        vehicle.setActive(true);
        return vehicleRepository.save(vehicle);
    }

    @Transactional
    public Vehicle updateVehicle(UUID id, UUID companyId, Vehicle updates) {
        Vehicle vehicle = getVehicle(id, companyId);
        vehicle.setName(updates.getName());
        vehicle.setFuelType(updates.getFuelType());
        vehicle.setConsumptionLoaded(updates.getConsumptionLoaded());
        vehicle.setConsumptionEmpty(updates.getConsumptionEmpty());
        vehicle.setTankCapacity(updates.getTankCapacity());
        vehicle.setEuroClass(updates.getEuroClass());
        vehicle.setMaintenanceCostPerKm(updates.getMaintenanceCostPerKm());
        vehicle.setTireCostPerKm(updates.getTireCostPerKm());
        vehicle.setDepreciationPerKm(updates.getDepreciationPerKm());
        vehicle.setInsurancePerDay(updates.getInsurancePerDay());
        return vehicleRepository.save(vehicle);
    }

    @Transactional
    public void deleteVehicle(UUID id, UUID companyId) {
        Vehicle vehicle = getVehicle(id, companyId);
        vehicle.setActive(false);
        vehicleRepository.save(vehicle);
    }
}
