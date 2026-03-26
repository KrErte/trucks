package eu.fuelfleet.route.dto;

import eu.fuelfleet.route.entity.SavedRoute;

import java.util.UUID;

public record SavedRouteResponse(
        UUID id,
        String name,
        String originAddress,
        String destinationAddress
) {
    public static SavedRouteResponse fromEntity(SavedRoute r) {
        return new SavedRouteResponse(r.getId(), r.getName(), r.getOriginAddress(), r.getDestinationAddress());
    }
}
