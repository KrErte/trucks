package eu.fuelfleet.calculation.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import eu.fuelfleet.calculation.entity.Calculation;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Service
public class PdfService {

    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 18, Font.BOLD, new Color(26, 35, 126));
    private static final Font HEADER_FONT = new Font(Font.HELVETICA, 11, Font.BOLD, Color.WHITE);
    private static final Font NORMAL_FONT = new Font(Font.HELVETICA, 10, Font.NORMAL);
    private static final Font BOLD_FONT = new Font(Font.HELVETICA, 10, Font.BOLD);
    private static final Font PROFIT_FONT = new Font(Font.HELVETICA, 14, Font.BOLD);

    public byte[] generateCalculationPdf(Calculation calc) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 50, 50, 50, 50);

        try {
            PdfWriter.getInstance(doc, baos);
            doc.open();

            // Title
            Paragraph title = new Paragraph("FuelFleet - Kalkulatsioon", TITLE_FONT);
            title.setSpacingAfter(20);
            doc.add(title);

            // Route info
            Paragraph route = new Paragraph();
            route.add(new Chunk("Marsruut: ", BOLD_FONT));
            route.add(new Chunk(calc.getOrigin() + " → " + calc.getDestination(), NORMAL_FONT));
            route.setSpacingAfter(5);
            doc.add(route);

            Paragraph vehicle = new Paragraph();
            vehicle.add(new Chunk("Sõiduk: ", BOLD_FONT));
            vehicle.add(new Chunk(calc.getVehicle().getName(), NORMAL_FONT));
            vehicle.setSpacingAfter(5);
            doc.add(vehicle);

            Paragraph dist = new Paragraph();
            dist.add(new Chunk("Distants: ", BOLD_FONT));
            dist.add(new Chunk(calc.getDistanceKm() + " km | " + calc.getEstimatedHours() + " h", NORMAL_FONT));
            dist.setSpacingAfter(5);
            doc.add(dist);

            if (calc.getDriverDays() != null && calc.getDriverDays() > 0) {
                Paragraph days = new Paragraph();
                days.add(new Chunk("Sõidupäevad: ", BOLD_FONT));
                days.add(new Chunk(String.valueOf(calc.getDriverDays()), NORMAL_FONT));
                days.setSpacingAfter(5);
                doc.add(days);
            }

            Paragraph date = new Paragraph();
            date.add(new Chunk("Kuupäev: ", BOLD_FONT));
            date.add(new Chunk(calc.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")), NORMAL_FONT));
            date.setSpacingAfter(20);
            doc.add(date);

            // Cost table
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3, 1});

            addHeaderCell(table, "Kulu");
            addHeaderCell(table, "Summa (EUR)");

            addRow(table, "Kütusekulu", calc.getFuelCost());
            addRow(table, "Teemaksud", calc.getTollCost());
            addRow(table, "Juhi kulu", calc.getDriverDailyCost());
            addRow(table, "Muud kulud", calc.getOtherCosts());
            if (calc.getMaintenanceCost() != null && calc.getMaintenanceCost().signum() > 0) {
                addRow(table, "Hoolduskulu", calc.getMaintenanceCost());
            }
            if (calc.getTireCost() != null && calc.getTireCost().signum() > 0) {
                addRow(table, "Rehvikulu", calc.getTireCost());
            }
            if (calc.getDepreciationCost() != null && calc.getDepreciationCost().signum() > 0) {
                addRow(table, "Amortisatsioon", calc.getDepreciationCost());
            }
            if (calc.getInsuranceCost() != null && calc.getInsuranceCost().signum() > 0) {
                addRow(table, "Kindlustus", calc.getInsuranceCost());
            }

            if (calc.isIncludeReturnTrip() && calc.getReturnFuelCost() != null) {
                addRow(table, "Tagasisõidu kütusekulu", calc.getReturnFuelCost());
            }

            // Total row
            PdfPCell totalLabel = new PdfPCell(new Phrase("KOKKU", BOLD_FONT));
            totalLabel.setPadding(8);
            totalLabel.setBackgroundColor(new Color(240, 240, 240));
            table.addCell(totalLabel);
            PdfPCell totalValue = new PdfPCell(new Phrase(fmt(calc.getTotalCost()), BOLD_FONT));
            totalValue.setPadding(8);
            totalValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalValue.setBackgroundColor(new Color(240, 240, 240));
            table.addCell(totalValue);

            doc.add(table);

            // Profit
            doc.add(new Paragraph(" "));
            Paragraph orderP = new Paragraph();
            orderP.add(new Chunk("Tellimuse hind: ", BOLD_FONT));
            orderP.add(new Chunk(fmt(calc.getOrderPrice()) + " EUR", NORMAL_FONT));
            orderP.setSpacingAfter(5);
            doc.add(orderP);

            if (calc.getCostPerKm() != null && calc.getCostPerKm().signum() > 0) {
                Paragraph cpk = new Paragraph();
                cpk.add(new Chunk("Kulu km kohta: ", BOLD_FONT));
                cpk.add(new Chunk(fmt(calc.getCostPerKm()) + " EUR/km", NORMAL_FONT));
                cpk.setSpacingAfter(5);
                doc.add(cpk);
            }

            if (calc.getCo2EmissionsKg() != null && calc.getCo2EmissionsKg().signum() > 0) {
                Paragraph co2 = new Paragraph();
                co2.add(new Chunk("CO2 heitmed: ", BOLD_FONT));
                co2.add(new Chunk(fmt(calc.getCo2EmissionsKg()) + " kg", NORMAL_FONT));
                co2.setSpacingAfter(5);
                doc.add(co2);
            }

            boolean profitable = calc.getProfit() != null && calc.getProfit().signum() >= 0;
            PROFIT_FONT.setColor(profitable ? new Color(46, 125, 50) : new Color(198, 40, 40));
            Paragraph profitP = new Paragraph();
            profitP.add(new Chunk("Kasum: ", BOLD_FONT));
            profitP.add(new Chunk(fmt(calc.getProfit()) + " EUR (" + fmt(calc.getProfitMarginPct()) + "%)", PROFIT_FONT));
            doc.add(profitP);

            doc.close();
        } catch (Exception e) {
            throw new RuntimeException("PDF generation failed", e);
        }

        return baos.toByteArray();
    }

    private void addHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, HEADER_FONT));
        cell.setBackgroundColor(new Color(26, 35, 126));
        cell.setPadding(8);
        table.addCell(cell);
    }

    private void addRow(PdfPTable table, String label, BigDecimal value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, NORMAL_FONT));
        labelCell.setPadding(6);
        table.addCell(labelCell);
        PdfPCell valueCell = new PdfPCell(new Phrase(fmt(value), NORMAL_FONT));
        valueCell.setPadding(6);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(valueCell);
    }

    private String fmt(BigDecimal val) {
        return val != null ? val.setScale(2, java.math.RoundingMode.HALF_UP).toString() : "0.00";
    }
}
