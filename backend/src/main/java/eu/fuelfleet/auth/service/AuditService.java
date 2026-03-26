package eu.fuelfleet.auth.service;

import eu.fuelfleet.auth.entity.AuditLog;
import eu.fuelfleet.auth.repository.AuditLogRepository;
import eu.fuelfleet.company.entity.Company;
import eu.fuelfleet.company.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final CompanyService companyService;

    @Async
    public void log(UUID companyId, UUID userId, String action, String entityType, String entityId, String details, String ipAddress) {
        Company company = companyService.getCompany(companyId);
        AuditLog auditLog = AuditLog.builder()
                .company(company)
                .userId(userId)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .ipAddress(ipAddress)
                .build();
        auditLogRepository.save(auditLog);
    }

    public Page<AuditLog> getAuditLogs(UUID companyId, Pageable pageable) {
        return auditLogRepository.findByCompanyIdOrderByCreatedAtDesc(companyId, pageable);
    }
}
