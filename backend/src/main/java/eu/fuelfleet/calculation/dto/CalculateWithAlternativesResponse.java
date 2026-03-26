package eu.fuelfleet.calculation.dto;

import java.util.List;

public record CalculateWithAlternativesResponse(
        List<RouteAlternative> alternatives
) {}
