package eu.fuelfleet.pricing.controller;

import eu.fuelfleet.auth.security.UserPrincipal;
import eu.fuelfleet.pricing.dto.PricingSuggestRequest;
import eu.fuelfleet.pricing.dto.PricingSuggestionResponse;
import eu.fuelfleet.pricing.service.PricingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pricing")
@RequiredArgsConstructor
public class PricingController {

    private final PricingService pricingService;

    @PostMapping("/suggest")
    public ResponseEntity<PricingSuggestionResponse> suggest(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody PricingSuggestRequest request) {
        PricingSuggestionResponse response = pricingService.suggest(request, principal.getCompanyId());
        return ResponseEntity.ok(response);
    }
}
