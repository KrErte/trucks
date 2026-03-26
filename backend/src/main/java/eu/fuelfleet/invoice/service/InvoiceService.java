package eu.fuelfleet.invoice.service;

import eu.fuelfleet.calculation.entity.Calculation;
import eu.fuelfleet.company.entity.Company;
import eu.fuelfleet.invoice.entity.Invoice;
import eu.fuelfleet.invoice.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;

    @Transactional
    public Invoice createFromCalculation(Calculation calculation, String customerName, String customerVat, int dueDays) {
        Company company = calculation.getCompany();
        String invoiceNumber = generateInvoiceNumber(company.getId());

        Invoice invoice = Invoice.builder()
                .company(company)
                .calculation(calculation)
                .invoiceNumber(invoiceNumber)
                .customerName(customerName)
                .customerVat(customerVat)
                .amount(calculation.getOrderPrice())
                .currency(calculation.getCurrency() != null ? calculation.getCurrency() : "EUR")
                .issuedDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(dueDays))
                .status("DRAFT")
                .build();

        return invoiceRepository.save(invoice);
    }

    public List<Invoice> getInvoices(UUID companyId) {
        return invoiceRepository.findByCompanyIdOrderByCreatedAtDesc(companyId);
    }

    public Invoice getInvoice(UUID invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));
    }

    @Transactional
    public Invoice updateStatus(UUID invoiceId, String status) {
        Invoice invoice = getInvoice(invoiceId);
        invoice.setStatus(status);
        return invoiceRepository.save(invoice);
    }

    private String generateInvoiceNumber(UUID companyId) {
        Long seq = invoiceRepository.getNextInvoiceSequence(companyId);
        return String.format("FF-%d-%04d", Year.now().getValue(), seq);
    }

    public String exportEArveldajaCSV(UUID companyId) {
        List<Invoice> invoices = invoiceRepository.findByCompanyIdOrderByCreatedAtDesc(companyId);
        StringBuilder sb = new StringBuilder();
        sb.append("arve_nr;kuupaev;maksetahtaeg;ostja_nimi;ostja_kmkr;summa;valuuta;staatus\n");
        for (Invoice inv : invoices) {
            sb.append(inv.getInvoiceNumber()).append(";")
                    .append(inv.getIssuedDate()).append(";")
                    .append(inv.getDueDate()).append(";")
                    .append(inv.getCustomerName()).append(";")
                    .append(inv.getCustomerVat() != null ? inv.getCustomerVat() : "").append(";")
                    .append(inv.getAmount()).append(";")
                    .append(inv.getCurrency()).append(";")
                    .append(inv.getStatus()).append("\n");
        }
        return sb.toString();
    }

    public String exportMeritXML(UUID companyId) {
        List<Invoice> invoices = invoiceRepository.findByCompanyIdOrderByCreatedAtDesc(companyId);
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<Invoices>\n");
        for (Invoice inv : invoices) {
            sb.append("  <Invoice>\n")
                    .append("    <InvoiceNumber>").append(inv.getInvoiceNumber()).append("</InvoiceNumber>\n")
                    .append("    <InvoiceDate>").append(inv.getIssuedDate()).append("</InvoiceDate>\n")
                    .append("    <DueDate>").append(inv.getDueDate()).append("</DueDate>\n")
                    .append("    <CustomerName>").append(escapeXml(inv.getCustomerName())).append("</CustomerName>\n")
                    .append("    <CustomerVATNo>").append(inv.getCustomerVat() != null ? escapeXml(inv.getCustomerVat()) : "").append("</CustomerVATNo>\n")
                    .append("    <TotalAmount>").append(inv.getAmount()).append("</TotalAmount>\n")
                    .append("    <Currency>").append(inv.getCurrency()).append("</Currency>\n")
                    .append("    <Status>").append(inv.getStatus()).append("</Status>\n")
                    .append("  </Invoice>\n");
        }
        sb.append("</Invoices>");
        return sb.toString();
    }

    private String escapeXml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
