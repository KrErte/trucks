package eu.fuelfleet.calculation.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(
        BigDecimal totalProfit,
        long tripCount,
        BigDecimal avgMargin,
        List<MonthlyData> monthlyData,
        List<VehicleStats> vehicleStats
) {
    public record MonthlyData(
            String month,
            BigDecimal profit,
            long tripCount,
            BigDecimal avgMargin
    ) {}

    public record VehicleStats(
            String vehicleId,
            String vehicleName,
            long tripCount,
            BigDecimal totalProfit,
            BigDecimal avgMargin,
            BigDecimal totalDistanceKm
    ) {}
}
