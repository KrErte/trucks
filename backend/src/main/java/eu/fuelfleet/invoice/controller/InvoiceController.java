package eu.fuelfleet.invoice.controller;

import eu.fuelfleet.auth.security.UserPrincipal;
import eu.fuelfleet.calculation.entity.Calculation;
import eu.fuelfleet.calculation.service.CalculationService;
import eu.fuelfleet.invoice.entity.Invoice;
import eu.fuelfleet.invoice.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final CalculationService calculationService;

    @GetMapping
    public List<Invoice> getInvoices(@AuthenticationPrincipal UserPrincipal principal) {
        return invoiceService.getInvoices(principal.getCompanyId());
    }

    @PostMapping
    public Invoice createInvoice(@AuthenticationPrincipal UserPrincipal principal,
                                  @RequestBody Map<String, Object> body) {
        UUID calculationId = UUID.fromString((String) body.get("calculationId"));
        String customerName = (String) body.get("customerName");
        String customerVat = (String) body.getOrDefault("customerVat", null);
        int dueDays = body.containsKey("dueDays") ? (Integer) body.get("dueDays") : 14;

        Calculation calculation = calculationService.getCalculation(calculationId, principal.getCompanyId());
        return invoiceService.createFromCalculation(calculation, customerName, customerVat, dueDays);
    }

    @PutMapping("/{id}/status")
    public Invoice updateStatus(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        return invoiceService.updateStatus(id, body.get("status"));
    }

    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportCSV(@AuthenticationPrincipal UserPrincipal principal) {
        String csv = invoiceService.exportEArveldajaCSV(principal.getCompanyId());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=invoices-earveldaja.csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/export/xml")
    public ResponseEntity<byte[]> exportXML(@AuthenticationPrincipal UserPrincipal principal) {
        String xml = invoiceService.exportMeritXML(principal.getCompanyId());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=invoices-merit.xml")
                .contentType(MediaType.APPLICATION_XML)
                .body(xml.getBytes(StandardCharsets.UTF_8));
    }
}
