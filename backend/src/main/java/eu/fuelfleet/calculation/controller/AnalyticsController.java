package eu.fuelfleet.calculation.controller;

import eu.fuelfleet.auth.security.UserPrincipal;
import eu.fuelfleet.calculation.dto.CostTrendResponse;
import eu.fuelfleet.calculation.dto.LaneStatsResponse;
import eu.fuelfleet.calculation.entity.Calculation;
import eu.fuelfleet.calculation.service.AnalyticsService;
import eu.fuelfleet.calculation.service.ExcelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final ExcelService excelService;

    @GetMapping("/lanes")
    public List<LaneStatsResponse> getLaneStats(@AuthenticationPrincipal UserPrincipal principal) {
        return analyticsService.getLaneStats(principal.getCompanyId());
    }

    @GetMapping("/cost-trends")
    public List<CostTrendResponse> getCostTrends(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "12") int months) {
        return analyticsService.getCostTrends(principal.getCompanyId(), months);
    }

    @GetMapping("/report")
    public List<Calculation> getMonthlyReport(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam String month) {
        return analyticsService.getMonthlyReport(principal.getCompanyId(), month);
    }

    @GetMapping("/report/export")
    public ResponseEntity<byte[]> exportReport(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam String month) {
        List<Calculation> calcs = analyticsService.getMonthlyReport(principal.getCompanyId(), month);
        byte[] excel = excelService.generateBatchExcel(calcs);
        return ResponseEntity.ok()
                .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .header("Content-Disposition", "attachment; filename=report-" + month + ".xlsx")
                .body(excel);
    }
}
