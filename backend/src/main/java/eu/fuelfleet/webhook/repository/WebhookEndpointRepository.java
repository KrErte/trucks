package eu.fuelfleet.webhook.repository;

import eu.fuelfleet.webhook.entity.WebhookEndpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WebhookEndpointRepository extends JpaRepository<WebhookEndpoint, UUID> {
    List<WebhookEndpoint> findByCompanyIdAndActiveTrue(UUID companyId);
    List<WebhookEndpoint> findByCompanyId(UUID companyId);
}
