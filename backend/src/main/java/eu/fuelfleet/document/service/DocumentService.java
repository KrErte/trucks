package eu.fuelfleet.document.service;

import eu.fuelfleet.auth.entity.User;
import eu.fuelfleet.auth.repository.UserRepository;
import eu.fuelfleet.document.dto.DocumentRequest;
import eu.fuelfleet.document.dto.DocumentResponse;
import eu.fuelfleet.document.entity.VehicleDocument;
import eu.fuelfleet.document.repository.VehicleDocumentRepository;
import eu.fuelfleet.driver.repository.DriverRepository;
import eu.fuelfleet.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {
    private final VehicleDocumentRepository documentRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final UserRepository userRepository;

    public List<DocumentResponse> getAll() {
        return documentRepository.findByCompanyIdOrderByExpiryDateAsc(getCompanyId()).stream()
                .map(DocumentResponse::from).toList();
    }

    @Transactional
    public DocumentResponse create(DocumentRequest req) {
        User user = getCurrentUser();
        VehicleDocument doc = VehicleDocument.builder()
                .company(user.getCompany())
                .vehicle(req.getVehicleId() != null ? vehicleRepository.findById(req.getVehicleId()).orElse(null) : null)
                .driver(req.getDriverId() != null ? driverRepository.findById(req.getDriverId()).orElse(null) : null)
                .type(req.getType()).name(req.getName())
                .expiryDate(req.getExpiryDate()).issueDate(req.getIssueDate())
                .documentNumber(req.getDocumentNumber()).notes(req.getNotes())
                .build();
        return DocumentResponse.from(documentRepository.save(doc));
    }

    @Transactional
    public DocumentResponse update(UUID id, DocumentRequest req) {
        VehicleDocument doc = documentRepository.findById(id)
                .filter(d -> d.getCompany().getId().equals(getCompanyId()))
                .orElseThrow(() -> new RuntimeException("Document not found"));
        if (req.getVehicleId() != null) doc.setVehicle(vehicleRepository.findById(req.getVehicleId()).orElse(null));
        if (req.getDriverId() != null) doc.setDriver(driverRepository.findById(req.getDriverId()).orElse(null));
        doc.setType(req.getType()); doc.setName(req.getName());
        doc.setExpiryDate(req.getExpiryDate()); doc.setIssueDate(req.getIssueDate());
        doc.setDocumentNumber(req.getDocumentNumber()); doc.setNotes(req.getNotes());
        return DocumentResponse.from(documentRepository.save(doc));
    }

    @Transactional
    public void delete(UUID id) {
        documentRepository.findById(id)
                .filter(d -> d.getCompany().getId().equals(getCompanyId()))
                .ifPresent(documentRepository::delete);
    }

    private UUID getCompanyId() { return getCurrentUser().getCompany().getId(); }
    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElseThrow();
    }
}
