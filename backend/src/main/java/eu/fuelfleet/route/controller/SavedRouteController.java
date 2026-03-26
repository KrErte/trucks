package eu.fuelfleet.route.controller;

import eu.fuelfleet.auth.entity.User;
import eu.fuelfleet.auth.repository.UserRepository;
import eu.fuelfleet.auth.security.UserPrincipal;
import eu.fuelfleet.company.service.CompanyService;
import eu.fuelfleet.route.dto.SavedRouteRequest;
import eu.fuelfleet.route.dto.SavedRouteResponse;
import eu.fuelfleet.route.entity.SavedRoute;
import eu.fuelfleet.route.repository.SavedRouteRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/saved-routes")
@RequiredArgsConstructor
public class SavedRouteController {

    private final SavedRouteRepository savedRouteRepository;
    private final CompanyService companyService;
    private final UserRepository userRepository;

    @GetMapping
    public List<SavedRouteResponse> getRoutes(@AuthenticationPrincipal UserPrincipal principal) {
        return savedRouteRepository.findByCompanyIdOrderByNameAsc(principal.getCompanyId())
                .stream().map(SavedRouteResponse::fromEntity).toList();
    }

    @PostMapping
    public ResponseEntity<SavedRouteResponse> createRoute(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SavedRouteRequest request) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        SavedRoute route = SavedRoute.builder()
                .company(companyService.getCompany(principal.getCompanyId()))
                .user(user)
                .name(request.name())
                .originAddress(request.originAddress())
                .destinationAddress(request.destinationAddress())
                .build();
        route = savedRouteRepository.save(route);
        return ResponseEntity.status(HttpStatus.CREATED).body(SavedRouteResponse.fromEntity(route));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoute(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        SavedRoute route = savedRouteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!route.getCompany().getId().equals(principal.getCompanyId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        savedRouteRepository.delete(route);
        return ResponseEntity.noContent().build();
    }
}
