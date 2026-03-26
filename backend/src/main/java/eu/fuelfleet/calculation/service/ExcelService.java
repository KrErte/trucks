package eu.fuelfleet.calculation.service;

import eu.fuelfleet.calculation.entity.Calculation;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExcelService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public byte[] generateCalculationExcel(Calculation calc) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Kalkulatsioon");
            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle boldStyle = createBoldStyle(wb);
            int row = 0;

            // Title
            Row titleRow = sheet.createRow(row++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("FuelFleet - Kalkulatsioon");
            titleCell.setCellStyle(boldStyle);
            row++;

            // Info
            row = addInfoRow(sheet, row, "Marsruut", calc.getOrigin() + " → " + calc.getDestination());
            row = addInfoRow(sheet, row, "Sõiduk", calc.getVehicle().getName());
            row = addInfoRow(sheet, row, "Distants", calc.getDistanceKm() + " km");
            row = addInfoRow(sheet, row, "Aeg", calc.getEstimatedHours() + " h");
            row = addInfoRow(sheet, row, "Kuupäev", calc.getCreatedAt().format(DATE_FMT));
            row++;

            // Cost table header
            Row hdr = sheet.createRow(row++);
            createCell(hdr, 0, "Kulu", headerStyle);
            createCell(hdr, 1, "Summa (EUR)", headerStyle);

            row = addCostRow(sheet, row, "Kütusekulu", calc.getFuelCost());
            row = addCostRow(sheet, row, "Teemaksud", calc.getTollCost());
            row = addCostRow(sheet, row, "Juhi kulu", calc.getDriverDailyCost());
            row = addCostRow(sheet, row, "Muud kulud", calc.getOtherCosts());
            if (calc.getMaintenanceCost() != null && calc.getMaintenanceCost().signum() > 0) {
                row = addCostRow(sheet, row, "Hoolduskulu", calc.getMaintenanceCost());
            }
            if (calc.getTireCost() != null && calc.getTireCost().signum() > 0) {
                row = addCostRow(sheet, row, "Rehvikulu", calc.getTireCost());
            }
            if (calc.getDepreciationCost() != null && calc.getDepreciationCost().signum() > 0) {
                row = addCostRow(sheet, row, "Amortisatsioon", calc.getDepreciationCost());
            }
            if (calc.getInsuranceCost() != null && calc.getInsuranceCost().signum() > 0) {
                row = addCostRow(sheet, row, "Kindlustus", calc.getInsuranceCost());
            }
            if (calc.isIncludeReturnTrip() && calc.getReturnFuelCost() != null) {
                row = addCostRow(sheet, row, "Tagasisõidu kütusekulu", calc.getReturnFuelCost());
            }
            row = addCostRow(sheet, row, "KOKKU", calc.getTotalCost());
            row++;

            row = addInfoRow(sheet, row, "Tellimuse hind", fmt(calc.getOrderPrice()) + " EUR");
            row = addInfoRow(sheet, row, "Kasum", fmt(calc.getProfit()) + " EUR");
            row = addInfoRow(sheet, row, "Marginaal", fmt(calc.getProfitMarginPct()) + "%");
            if (calc.getCostPerKm() != null) {
                row = addInfoRow(sheet, row, "Kulu km kohta", fmt(calc.getCostPerKm()) + " EUR/km");
            }
            if (calc.getDriverDays() != null) {
                row = addInfoRow(sheet, row, "Sõidupäevad", String.valueOf(calc.getDriverDays()));
            }
            if (calc.getCo2EmissionsKg() != null) {
                addInfoRow(sheet, row, "CO2 heitmed", fmt(calc.getCo2EmissionsKg()) + " kg");
            }

            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);

            return toBytes(wb);
        } catch (Exception e) {
            throw new RuntimeException("Excel generation failed", e);
        }
    }

    public byte[] generateBatchExcel(List<Calculation> calcs) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Kalkulatsioonid");
            CellStyle headerStyle = createHeaderStyle(wb);
            int row = 0;

            String[] headers = {"Kuupäev", "Lähtekoht", "Sihtkoht", "Sõiduk", "Distants (km)", "Aeg (h)",
                    "Kütusekulu", "Teemaksud", "Juhi kulu", "Muud kulud", "Tagasisõit", "Kokku",
                    "Tellimuse hind", "Kasum", "Marginaal %", "Kulu/km", "Sõidupäevad", "CO2 (kg)"};
            Row hdr = sheet.createRow(row++);
            for (int i = 0; i < headers.length; i++) {
                createCell(hdr, i, headers[i], headerStyle);
            }

            for (Calculation c : calcs) {
                Row r = sheet.createRow(row++);
                int col = 0;
                r.createCell(col++).setCellValue(c.getCreatedAt().format(DATE_FMT));
                r.createCell(col++).setCellValue(c.getOrigin());
                r.createCell(col++).setCellValue(c.getDestination());
                r.createCell(col++).setCellValue(c.getVehicle().getName());
                r.createCell(col++).setCellValue(toDouble(c.getDistanceKm()));
                r.createCell(col++).setCellValue(toDouble(c.getEstimatedHours()));
                r.createCell(col++).setCellValue(toDouble(c.getFuelCost()));
                r.createCell(col++).setCellValue(toDouble(c.getTollCost()));
                r.createCell(col++).setCellValue(toDouble(c.getDriverDailyCost()));
                r.createCell(col++).setCellValue(toDouble(c.getOtherCosts()));
                r.createCell(col++).setCellValue(toDouble(c.getReturnFuelCost()));
                r.createCell(col++).setCellValue(toDouble(c.getTotalCost()));
                r.createCell(col++).setCellValue(toDouble(c.getOrderPrice()));
                r.createCell(col++).setCellValue(toDouble(c.getProfit()));
                r.createCell(col++).setCellValue(toDouble(c.getProfitMarginPct()));
                r.createCell(col++).setCellValue(toDouble(c.getCostPerKm()));
                r.createCell(col++).setCellValue(c.getDriverDays() != null ? c.getDriverDays() : 0);
                r.createCell(col).setCellValue(toDouble(c.getCo2EmissionsKg()));
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            return toBytes(wb);
        } catch (Exception e) {
            throw new RuntimeException("Excel generation failed", e);
        }
    }

    private int addInfoRow(Sheet sheet, int rowIdx, String label, String value) {
        Row row = sheet.createRow(rowIdx);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value);
        return rowIdx + 1;
    }

    private int addCostRow(Sheet sheet, int rowIdx, String label, BigDecimal value) {
        Row row = sheet.createRow(rowIdx);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(toDouble(value));
        return rowIdx + 1;
    }

    private void createCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle createBoldStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        return style;
    }

    private double toDouble(BigDecimal val) {
        return val != null ? val.setScale(2, RoundingMode.HALF_UP).doubleValue() : 0.0;
    }

    private String fmt(BigDecimal val) {
        return val != null ? val.setScale(2, RoundingMode.HALF_UP).toString() : "0.00";
    }

    private byte[] toBytes(XSSFWorkbook wb) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        wb.write(baos);
        return baos.toByteArray();
    }
}
