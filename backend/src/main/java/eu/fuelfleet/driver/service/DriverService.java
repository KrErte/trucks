package eu.fuelfleet.driver.service;

import eu.fuelfleet.auth.entity.User;
import eu.fuelfleet.auth.repository.UserRepository;
import eu.fuelfleet.driver.dto.DriverRequest;
import eu.fuelfleet.driver.dto.DriverResponse;
import eu.fuelfleet.driver.entity.Driver;
import eu.fuelfleet.driver.repository.DriverRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DriverService {
    private final DriverRepository driverRepository;
    private final UserRepository userRepository;

    public List<DriverResponse> getAll() {
        UUID companyId = getCompanyId();
        return driverRepository.findByCompanyId(companyId).stream()
                .map(DriverResponse::from)
                .toList();
    }

    public List<DriverResponse> getActive() {
        UUID companyId = getCompanyId();
        return driverRepository.findByCompanyIdAndActiveTrue(companyId).stream()
                .map(DriverResponse::from)
                .toList();
    }

    @Transactional
    public DriverResponse create(DriverRequest req) {
        User user = getCurrentUser();
        Driver driver = Driver.builder()
                .company(user.getCompany())
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .email(req.getEmail())
                .phone(req.getPhone())
                .licenseNumber(req.getLicenseNumber())
                .licenseExpiry(req.getLicenseExpiry())
                .licenseCategories(req.getLicenseCategories())
                .idCardNumber(req.getIdCardNumber())
                .idCardExpiry(req.getIdCardExpiry())
                .adrCertificateExpiry(req.getAdrCertificateExpiry())
                .driverCardNumber(req.getDriverCardNumber())
                .driverCardExpiry(req.getDriverCardExpiry())
                .dailyRate(req.getDailyRate())
                .notes(req.getNotes())
                .active(true)
                .build();
        return DriverResponse.from(driverRepository.save(driver));
    }

    @Transactional
    public DriverResponse update(UUID id, DriverRequest req) {
        Driver driver = driverRepository.findById(id)
                .filter(d -> d.getCompany().getId().equals(getCompanyId()))
                .orElseThrow(() -> new RuntimeException("Driver not found"));
        driver.setFirstName(req.getFirstName());
        driver.setLastName(req.getLastName());
        driver.setEmail(req.getEmail());
        driver.setPhone(req.getPhone());
        driver.setLicenseNumber(req.getLicenseNumber());
        driver.setLicenseExpiry(req.getLicenseExpiry());
        driver.setLicenseCategories(req.getLicenseCategories());
        driver.setIdCardNumber(req.getIdCardNumber());
        driver.setIdCardExpiry(req.getIdCardExpiry());
        driver.setAdrCertificateExpiry(req.getAdrCertificateExpiry());
        driver.setDriverCardNumber(req.getDriverCardNumber());
        driver.setDriverCardExpiry(req.getDriverCardExpiry());
        driver.setDailyRate(req.getDailyRate());
        driver.setNotes(req.getNotes());
        return DriverResponse.from(driverRepository.save(driver));
    }

    @Transactional
    public void deactivate(UUID id) {
        driverRepository.findById(id)
                .filter(d -> d.getCompany().getId().equals(getCompanyId()))
                .ifPresent(d -> { d.setActive(false); driverRepository.save(d); });
    }

    @Transactional
    public void activate(UUID id) {
        driverRepository.findById(id)
                .filter(d -> d.getCompany().getId().equals(getCompanyId()))
                .ifPresent(d -> { d.setActive(true); driverRepository.save(d); });
    }

    @Transactional
    public void delete(UUID id) {
        driverRepository.findById(id)
                .filter(d -> d.getCompany().getId().equals(getCompanyId()))
                .ifPresent(driverRepository::delete);
    }

    private UUID getCompanyId() {
        return getCurrentUser().getCompany().getId();
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
    }
}
