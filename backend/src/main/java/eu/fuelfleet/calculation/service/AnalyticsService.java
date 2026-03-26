package eu.fuelfleet.calculation.service;

import eu.fuelfleet.calculation.dto.CostTrendResponse;
import eu.fuelfleet.calculation.dto.LaneStatsResponse;
import eu.fuelfleet.calculation.entity.Calculation;
import eu.fuelfleet.calculation.repository.CalculationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final CalculationRepository calculationRepository;
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    @Transactional(readOnly = true)
    public List<LaneStatsResponse> getLaneStats(UUID companyId) {
        return calculationRepository.findLaneStats(companyId).stream()
                .map(row -> new LaneStatsResponse(
                        (String) row[0],
                        (String) row[1],
                        (Long) row[2],
                        toBigDecimal(row[3]),
                        toBigDecimal(row[4]),
                        toBigDecimal(row[5]),
                        toBigDecimal(row[6])
                )).toList();
    }

    @Transactional(readOnly = true)
    public List<CostTrendResponse> getCostTrends(UUID companyId, int months) {
        LocalDateTime since = LocalDateTime.now().minusMonths(months);
        List<Calculation> calcs = calculationRepository.findByCompanyIdAndCreatedAtAfter(companyId, since);

        Map<String, List<Calculation>> byMonth = calcs.stream()
                .collect(Collectors.groupingBy(c -> c.getCreatedAt().format(MONTH_FMT), TreeMap::new, Collectors.toList()));

        return byMonth.entrySet().stream().map(e -> {
            List<Calculation> mc = e.getValue();
            int count = mc.size();
            BigDecimal divisor = BigDecimal.valueOf(count);
            return new CostTrendResponse(
                    e.getKey(),
                    avg(mc, Calculation::getFuelCost, divisor),
                    avg(mc, Calculation::getTollCost, divisor),
                    avg(mc, Calculation::getDriverDailyCost, divisor),
                    avg(mc, Calculation::getTotalCost, divisor),
                    avg(mc, Calculation::getOrderPrice, divisor),
                    avg(mc, Calculation::getProfit, divisor),
                    count
            );
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<Calculation> getMonthlyReport(UUID companyId, String yearMonth) {
        LocalDateTime start = java.time.YearMonth.parse(yearMonth).atDay(1).atStartOfDay();
        LocalDateTime end = start.plusMonths(1);
        return calculationRepository.findByCompanyIdAndCreatedAtBetween(companyId, start, end);
    }

    private BigDecimal avg(List<Calculation> calcs, java.util.function.Function<Calculation, BigDecimal> getter, BigDecimal divisor) {
        BigDecimal sum = calcs.stream()
                .map(getter)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return divisor.signum() == 0 ? BigDecimal.ZERO : sum.divide(divisor, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal toBigDecimal(Object val) {
        if (val == null) return BigDecimal.ZERO;
        if (val instanceof BigDecimal) return (BigDecimal) val;
        if (val instanceof Double) return BigDecimal.valueOf((Double) val).setScale(2, RoundingMode.HALF_UP);
        return new BigDecimal(val.toString()).setScale(2, RoundingMode.HALF_UP);
    }
}
