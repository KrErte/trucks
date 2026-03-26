package eu.fuelfleet.webhook.controller;

import eu.fuelfleet.auth.security.UserPrincipal;
import eu.fuelfleet.company.entity.Company;
import eu.fuelfleet.company.service.CompanyService;
import eu.fuelfleet.webhook.entity.WebhookDelivery;
import eu.fuelfleet.webhook.entity.WebhookEndpoint;
import eu.fuelfleet.webhook.service.WebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class WebhookEndpointController {

    private final WebhookService webhookService;
    private final CompanyService companyService;

    @GetMapping
    public List<WebhookEndpoint> getEndpoints(@AuthenticationPrincipal UserPrincipal principal) {
        return webhookService.getEndpoints(principal.getCompanyId());
    }

    @PostMapping
    public WebhookEndpoint createEndpoint(@AuthenticationPrincipal UserPrincipal principal,
                                           @RequestBody Map<String, Object> body) {
        Company company = companyService.getCompany(principal.getCompanyId());
        String url = (String) body.get("url");
        @SuppressWarnings("unchecked")
        List<String> eventsList = (List<String>) body.get("events");
        String[] events = eventsList != null ? eventsList.toArray(new String[0]) : new String[]{};
        return webhookService.createEndpoint(company, url, events);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEndpoint(@AuthenticationPrincipal UserPrincipal principal,
                                                @PathVariable UUID id) {
        webhookService.deleteEndpoint(id, principal.getCompanyId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/toggle")
    public WebhookEndpoint toggleEndpoint(@AuthenticationPrincipal UserPrincipal principal,
                                           @PathVariable UUID id) {
        return webhookService.toggleEndpoint(id, principal.getCompanyId());
    }

    @GetMapping("/{id}/deliveries")
    public Page<WebhookDelivery> getDeliveries(@PathVariable UUID id,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        return webhookService.getDeliveries(id, PageRequest.of(page, size));
    }
}
