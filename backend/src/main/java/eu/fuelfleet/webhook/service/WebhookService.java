package eu.fuelfleet.webhook.service;

import eu.fuelfleet.company.entity.Company;
import eu.fuelfleet.webhook.entity.WebhookDelivery;
import eu.fuelfleet.webhook.entity.WebhookEndpoint;
import eu.fuelfleet.webhook.repository.WebhookDeliveryRepository;
import eu.fuelfleet.webhook.repository.WebhookEndpointRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

    private final WebhookEndpointRepository endpointRepository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Transactional
    public WebhookEndpoint createEndpoint(Company company, String url, String[] events) {
        String secret = generateSecret();
        WebhookEndpoint endpoint = WebhookEndpoint.builder()
                .company(company)
                .url(url)
                .secret(secret)
                .events(events)
                .active(true)
                .build();
        return endpointRepository.save(endpoint);
    }

    public List<WebhookEndpoint> getEndpoints(UUID companyId) {
        return endpointRepository.findByCompanyId(companyId);
    }

    @Transactional
    public void deleteEndpoint(UUID endpointId, UUID companyId) {
        WebhookEndpoint endpoint = endpointRepository.findById(endpointId)
                .orElseThrow(() -> new RuntimeException("Endpoint not found"));
        if (!endpoint.getCompany().getId().equals(companyId)) {
            throw new RuntimeException("Access denied");
        }
        endpointRepository.delete(endpoint);
    }

    @Transactional
    public WebhookEndpoint toggleEndpoint(UUID endpointId, UUID companyId) {
        WebhookEndpoint endpoint = endpointRepository.findById(endpointId)
                .orElseThrow(() -> new RuntimeException("Endpoint not found"));
        if (!endpoint.getCompany().getId().equals(companyId)) {
            throw new RuntimeException("Access denied");
        }
        endpoint.setActive(!endpoint.isActive());
        return endpointRepository.save(endpoint);
    }

    public Page<WebhookDelivery> getDeliveries(UUID endpointId, Pageable pageable) {
        return deliveryRepository.findByEndpointIdOrderByCreatedAtDesc(endpointId, pageable);
    }

    @Async
    public void dispatch(UUID companyId, String eventType, Object payload) {
        List<WebhookEndpoint> endpoints = endpointRepository.findByCompanyIdAndActiveTrue(companyId);
        for (WebhookEndpoint endpoint : endpoints) {
            if (endpoint.getEvents() != null && !Arrays.asList(endpoint.getEvents()).contains(eventType)) {
                continue;
            }
            deliverWithRetry(endpoint, eventType, payload, 0);
        }
    }

    private void deliverWithRetry(WebhookEndpoint endpoint, String eventType, Object payload, int attempt) {
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(Map.of(
                    "event", eventType,
                    "data", payload,
                    "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            log.error("Failed to serialize webhook payload", e);
            return;
        }

        String signature = sign(payloadJson, endpoint.getSecret());

        WebhookDelivery delivery = WebhookDelivery.builder()
                .endpoint(endpoint)
                .eventType(eventType)
                .payload(payloadJson)
                .retryCount(attempt)
                .build();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Webhook-Signature", signature);
            headers.set("X-Webhook-Event", eventType);

            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint.getUrl(), HttpMethod.POST,
                    new HttpEntity<>(payloadJson, headers), String.class);

            delivery.setResponseStatus(response.getStatusCode().value());
            delivery.setFailed(false);
            deliveryRepository.save(delivery);
        } catch (Exception e) {
            log.warn("Webhook delivery failed for endpoint {} attempt {}: {}", endpoint.getId(), attempt, e.getMessage());
            delivery.setFailed(true);
            delivery.setResponseStatus(0);
            deliveryRepository.save(delivery);

            if (attempt < 3) {
                try {
                    Thread.sleep((long) Math.pow(2, attempt) * 1000);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                deliverWithRetry(endpoint, eventType, payload, attempt + 1);
            }
        }
    }

    private String sign(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign webhook payload", e);
        }
    }

    private String generateSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return "whsec_" + HexFormat.of().formatHex(bytes);
    }
}
