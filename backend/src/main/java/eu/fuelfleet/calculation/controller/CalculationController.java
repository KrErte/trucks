package eu.fuelfleet.calculation.controller;

import eu.fuelfleet.auth.security.UserPrincipal;
import eu.fuelfleet.calculation.dto.CalculateRequest;
import eu.fuelfleet.calculation.dto.CalculateWithAlternativesResponse;
import eu.fuelfleet.calculation.dto.CalculationResponse;
import eu.fuelfleet.calculation.dto.RouteAlternative;
import eu.fuelfleet.calculation.entity.Calculation;
import eu.fuelfleet.calculation.service.CalculationService;
import eu.fuelfleet.calculation.service.ExcelService;
import eu.fuelfleet.calculation.service.PdfService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CalculationController {

    private final CalculationService calculationService;
    private final PdfService pdfService;
    private final ExcelService excelService;
    private final eu.fuelfleet.calculation.service.GoogleMapsService googleMapsService;

    @GetMapping("/places/autocomplete")
    public ResponseEntity<List<Map<String, String>>> placesAutocomplete(@RequestParam String input) {
        if (input == null || input.length() < 2) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(googleMapsService.autocomplete(input));
    }

    @PostMapping("/calculations/preview")
    public ResponseEntity<CalculateWithAlternativesResponse> previewAlternatives(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CalculateRequest request) {
        var alternatives = calculationService.previewAlternatives(request, principal.getCompanyId());
        return ResponseEntity.ok(new CalculateWithAlternativesResponse(alternatives));
    }

    @PostMapping("/calculate")
    public ResponseEntity<CalculationResponse> calculate(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CalculateRequest request) {
        Calculation calculation = calculationService.calculate(
                request, principal.getCompanyId(), principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CalculationResponse.fromEntity(calculation));
    }

    @GetMapping("/calculations")
    public Page<CalculationResponse> getCalculations(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return calculationService.getHistoryResponse(principal.getCompanyId(), PageRequest.of(page, size));
    }

    @GetMapping("/calculations/{id}")
    public CalculationResponse getCalculation(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        Calculation calculation = calculationService.getCalculation(id, principal.getCompanyId());
        return CalculationResponse.fromEntity(calculation);
    }

    @GetMapping("/calculations/{id}/pdf")
    public ResponseEntity<byte[]> getCalculationPdf(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        Calculation calculation = calculationService.getCalculation(id, principal.getCompanyId());
        byte[] pdf = pdfService.generateCalculationPdf(calculation);
        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=calculation-" + id + ".pdf")
                .body(pdf);
    }

    @GetMapping("/calculations/{id}/excel")
    public ResponseEntity<byte[]> getCalculationExcel(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        Calculation calculation = calculationService.getCalculation(id, principal.getCompanyId());
        byte[] excel = excelService.generateCalculationExcel(calculation);
        return ResponseEntity.ok()
                .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .header("Content-Disposition", "attachment; filename=calculation-" + id + ".xlsx")
                .body(excel);
    }

    @GetMapping("/calculations/summary")
    public List<Map<String, Object>> getMonthlySummary(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "12") int months) {
        LocalDateTime since = LocalDateTime.now().minusMonths(months);
        List<Calculation> calcs = calculationService.getCalculationsSince(principal.getCompanyId(), since);

        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("yyyy-MM");
        Map<String, List<Calculation>> byMonth = calcs.stream()
                .collect(Collectors.groupingBy(c -> c.getCreatedAt().format(monthFmt),
                        TreeMap::new, Collectors.toList()));

        return byMonth.entrySet().stream().map(e -> {
            List<Calculation> mc = e.getValue();
            BigDecimal totalRevenue = mc.stream().map(Calculation::getOrderPrice).filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalCost = mc.stream().map(Calculation::getTotalCost).filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalProfit = mc.stream().map(Calculation::getProfit).filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal avgMargin = mc.stream().map(Calculation::getProfitMarginPct).filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(mc.size()), 2, RoundingMode.HALF_UP);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("month", e.getKey());
            row.put("tripCount", mc.size());
            row.put("totalRevenue", totalRevenue);
            row.put("totalCost", totalCost);
            row.put("totalProfit", totalProfit);
            row.put("avgMargin", avgMargin);
            return row;
        }).toList();
    }

    @GetMapping("/calculations/export/excel")
    public ResponseEntity<byte[]> exportAllExcel(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<Calculation> calculations = calculationService.getHistory(
                principal.getCompanyId(), org.springframework.data.domain.Pageable.unpaged()).getContent();
        byte[] excel = excelService.generateBatchExcel(calculations);
        return ResponseEntity.ok()
                .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .header("Content-Disposition", "attachment; filename=calculations-export.xlsx")
                .body(excel);
    }
}
