package eu.fuelfleet.maintenance.service;

import eu.fuelfleet.auth.entity.User;
import eu.fuelfleet.auth.repository.UserRepository;
import eu.fuelfleet.maintenance.dto.MaintenanceRequest;
import eu.fuelfleet.maintenance.dto.MaintenanceResponse;
import eu.fuelfleet.maintenance.entity.MaintenanceRecord;
import eu.fuelfleet.maintenance.repository.MaintenanceRepository;
import eu.fuelfleet.vehicle.entity.Vehicle;
import eu.fuelfleet.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MaintenanceService {
    private final MaintenanceRepository maintenanceRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;

    public List<MaintenanceResponse> getAll() {
        return maintenanceRepository.findByCompanyIdOrderByPerformedAtDesc(getCompanyId()).stream()
                .map(MaintenanceResponse::from).toList();
    }

    public List<MaintenanceResponse> getByVehicle(UUID vehicleId) {
        return maintenanceRepository.findByVehicleIdOrderByPerformedAtDesc(vehicleId).stream()
                .map(MaintenanceResponse::from).toList();
    }

    @Transactional
    public MaintenanceResponse create(MaintenanceRequest req) {
        User user = getCurrentUser();
        Vehicle vehicle = vehicleRepository.findById(req.getVehicleId())
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));
        MaintenanceRecord record = MaintenanceRecord.builder()
                .vehicle(vehicle).company(user.getCompany())
                .type(req.getType()).description(req.getDescription())
                .cost(req.getCost()).odometerKm(req.getOdometerKm())
                .performedAt(req.getPerformedAt()).nextDueDate(req.getNextDueDate())
                .nextDueKm(req.getNextDueKm()).performedBy(req.getPerformedBy())
                .notes(req.getNotes())
                .build();
        return MaintenanceResponse.from(maintenanceRepository.save(record));
    }

    @Transactional
    public MaintenanceResponse update(UUID id, MaintenanceRequest req) {
        MaintenanceRecord r = maintenanceRepository.findById(id)
                .filter(m -> m.getCompany().getId().equals(getCompanyId()))
                .orElseThrow(() -> new RuntimeException("Record not found"));
        if (req.getVehicleId() != null) {
            Vehicle v = vehicleRepository.findById(req.getVehicleId()).orElseThrow();
            r.setVehicle(v);
        }
        r.setType(req.getType()); r.setDescription(req.getDescription());
        r.setCost(req.getCost()); r.setOdometerKm(req.getOdometerKm());
        r.setPerformedAt(req.getPerformedAt()); r.setNextDueDate(req.getNextDueDate());
        r.setNextDueKm(req.getNextDueKm()); r.setPerformedBy(req.getPerformedBy());
        r.setNotes(req.getNotes());
        return MaintenanceResponse.from(maintenanceRepository.save(r));
    }

    @Transactional
    public void delete(UUID id) {
        maintenanceRepository.findById(id)
                .filter(m -> m.getCompany().getId().equals(getCompanyId()))
                .ifPresent(maintenanceRepository::delete);
    }

    private UUID getCompanyId() { return getCurrentUser().getCompany().getId(); }
    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElseThrow();
    }
}
