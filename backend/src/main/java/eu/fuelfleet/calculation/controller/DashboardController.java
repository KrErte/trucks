package eu.fuelfleet.calculation.controller;

import eu.fuelfleet.auth.security.UserPrincipal;
import eu.fuelfleet.calculation.dto.DashboardResponse;
import eu.fuelfleet.calculation.entity.Calculation;
import eu.fuelfleet.calculation.repository.CalculationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final CalculationRepository calculationRepository;

    @GetMapping
    public DashboardResponse getDashboard(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "6") int months) {

        LocalDateTime since = LocalDateTime.now().minusMonths(months);
        List<Calculation> calculations = calculationRepository.findByCompanyIdAndCreatedAtAfter(
                principal.getCompanyId(), since);

        BigDecimal totalProfit = calculations.stream()
                .map(Calculation::getProfit)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long tripCount = calculations.size();

        BigDecimal avgMargin = BigDecimal.ZERO;
        if (tripCount > 0) {
            avgMargin = calculations.stream()
                    .map(Calculation::getProfitMarginPct)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(tripCount), 2, RoundingMode.HALF_UP);
        }

        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("yyyy-MM");
        Map<String, List<Calculation>> byMonth = calculations.stream()
                .collect(Collectors.groupingBy(c -> c.getCreatedAt().format(monthFmt),
                        TreeMap::new, Collectors.toList()));

        List<DashboardResponse.MonthlyData> monthlyData = byMonth.entrySet().stream()
                .map(e -> {
                    List<Calculation> mc = e.getValue();
                    BigDecimal mp = mc.stream().map(Calculation::getProfit).filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal ma = mc.isEmpty() ? BigDecimal.ZERO :
                            mc.stream().map(Calculation::getProfitMarginPct).filter(Objects::nonNull)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                                    .divide(BigDecimal.valueOf(mc.size()), 2, RoundingMode.HALF_UP);
                    return new DashboardResponse.MonthlyData(e.getKey(), mp, mc.size(), ma);
                })
                .toList();

        // Vehicle stats
        Map<UUID, List<Calculation>> byVehicle = calculations.stream()
                .collect(Collectors.groupingBy(c -> c.getVehicle().getId()));

        List<DashboardResponse.VehicleStats> vehicleStats = byVehicle.entrySet().stream()
                .map(e -> {
                    List<Calculation> vc = e.getValue();
                    String vehicleName = vc.get(0).getVehicle().getName();
                    BigDecimal vProfit = vc.stream().map(Calculation::getProfit).filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal vMargin = vc.stream().map(Calculation::getProfitMarginPct).filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .divide(BigDecimal.valueOf(vc.size()), 2, RoundingMode.HALF_UP);
                    BigDecimal vDistance = vc.stream().map(Calculation::getDistanceKm).filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new DashboardResponse.VehicleStats(
                            e.getKey().toString(), vehicleName, vc.size(), vProfit, vMargin, vDistance);
                })
                .sorted((a, b) -> b.totalProfit().compareTo(a.totalProfit()))
                .toList();

        return new DashboardResponse(totalProfit, tripCount, avgMargin, monthlyData, vehicleStats);
    }
}
