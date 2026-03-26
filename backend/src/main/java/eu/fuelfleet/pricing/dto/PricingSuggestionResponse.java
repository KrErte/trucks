package eu.fuelfleet.pricing.dto;

public record PricingSuggestionResponse(
        SuggestedPriceRange priceRange,
        LaneIntelligence laneIntelligence,
        MarketContext marketContext
) {}
