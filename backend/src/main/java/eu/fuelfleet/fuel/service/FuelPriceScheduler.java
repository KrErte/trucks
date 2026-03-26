package eu.fuelfleet.fuel.service;

import eu.fuelfleet.fuel.entity.FuelPrice;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "fuel.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class FuelPriceScheduler {

    private static final Logger log = LoggerFactory.getLogger(FuelPriceScheduler.class);

    private final FuelPriceService fuelPriceService;
    private final RestTemplate restTemplate;

    @Scheduled(cron = "${fuel.scheduler.cron:0 0 6 * * *}")
    public void updateFuelPrices() {
        log.info("Starting scheduled fuel price update...");
        int updated = 0;

        // Estonian fuel prices from pistik.net
        updated += updateEstonianPrices();

        // European fuel prices from EU Oil Bulletin
        updated += updateEuropeanPrices();

        log.info("Fuel price update completed. {} prices updated.", updated);
    }

    /**
     * Manual trigger for fuel price refresh (called from admin controller).
     */
    public int refreshAll() {
        log.info("Manual fuel price refresh triggered.");
        int updated = 0;
        updated += updateEstonianPrices();
        updated += updateEuropeanPrices();
        log.info("Manual refresh completed. {} prices updated.", updated);
        return updated;
    }

    private int updateEstonianPrices() {
        int count = 0;
        try {
            Document doc = Jsoup.connect("https://www.pistik.net/kutusehinnad-eestis")
                    .userAgent("Mozilla/5.0 (FuelFleet Bot)")
                    .timeout(15000)
                    .get();

            // pistik.net has a table with fuel prices — look for diesel price
            Elements rows = doc.select("table tr");
            for (Element row : rows) {
                Elements cells = row.select("td");
                if (cells.size() >= 2) {
                    String label = cells.get(0).text().toLowerCase().trim();
                    String priceText = cells.get(1).text().trim()
                            .replaceAll("[^0-9.,]", "")
                            .replace(",", ".");

                    if (priceText.isEmpty()) continue;

                    try {
                        BigDecimal price = new BigDecimal(priceText);
                        // Sanity check: fuel prices should be between 0.5 and 5.0 EUR/L
                        if (price.compareTo(new BigDecimal("0.5")) < 0 || price.compareTo(new BigDecimal("5.0")) > 0) {
                            continue;
                        }

                        String fuelType = null;
                        if (label.contains("diesel") || label.contains("diisel")) {
                            fuelType = "DIESEL";
                        } else if (label.contains("95")) {
                            fuelType = "GASOLINE_95";
                        } else if (label.contains("98")) {
                            fuelType = "GASOLINE_98";
                        }

                        if (fuelType != null) {
                            savePrice("EE", fuelType, price, "pistik.net");
                            count++;
                            log.info("Updated EE {} price: {} EUR/L (pistik.net)", fuelType, price);
                        }
                    } catch (NumberFormatException e) {
                        // skip unparseable prices
                    }
                }
            }

            if (count == 0) {
                log.warn("No Estonian prices parsed from pistik.net, trying fallback...");
                count += updateEstonianPricesFallback();
            }
        } catch (Exception e) {
            log.error("Failed to fetch Estonian fuel prices from pistik.net: {}", e.getMessage());
            count += updateEstonianPricesFallback();
        }
        return count;
    }

    private int updateEstonianPricesFallback() {
        int count = 0;
        try {
            Document doc = Jsoup.connect("https://www.olerex.ee/kutusehinnad")
                    .userAgent("Mozilla/5.0 (FuelFleet Bot)")
                    .timeout(15000)
                    .get();

            // Olerex page has fuel prices in various formats
            String bodyText = doc.body().text();
            // Try to find diesel price pattern like "Diesel 1.XXX" or "Diislikütus 1.XXX"
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                    "(?i)(diesel|diisel|diislik[üu]tus)\\s*[:\\-]?\\s*(\\d+[.,]\\d{2,3})"
            );
            java.util.regex.Matcher matcher = pattern.matcher(bodyText);
            if (matcher.find()) {
                String priceStr = matcher.group(2).replace(",", ".");
                BigDecimal price = new BigDecimal(priceStr);
                if (price.compareTo(new BigDecimal("0.5")) >= 0 && price.compareTo(new BigDecimal("5.0")) <= 0) {
                    savePrice("EE", "DIESEL", price, "olerex.ee");
                    count++;
                    log.info("Updated EE DIESEL price: {} EUR/L (olerex.ee fallback)", price);
                }
            }
        } catch (Exception e) {
            log.error("Fallback also failed for Estonian prices: {}", e.getMessage());
        }
        return count;
    }

    private int updateEuropeanPrices() {
        int count = 0;
        try {
            // EU Weekly Oil Bulletin CSV data
            String url = "https://ec.europa.eu/energy/observatory/reports/latest_prices.csv";
            String csv = restTemplate.getForObject(url, String.class);

            if (csv == null || csv.isBlank()) {
                log.warn("Empty response from EU Oil Bulletin");
                return 0;
            }

            // Map of EU country codes we're interested in
            Map<String, String> countryMapping = new LinkedHashMap<>();
            countryMapping.put("DE", "DE");
            countryMapping.put("FI", "FI");
            countryMapping.put("LV", "LV");
            countryMapping.put("LT", "LT");
            countryMapping.put("PL", "PL");
            countryMapping.put("SE", "SE");
            countryMapping.put("FR", "FR");
            countryMapping.put("NL", "NL");
            countryMapping.put("BE", "BE");
            countryMapping.put("AT", "AT");
            countryMapping.put("IT", "IT");
            countryMapping.put("ES", "ES");

            String[] lines = csv.split("\n");
            if (lines.length < 2) {
                log.warn("EU Oil Bulletin CSV has insufficient data");
                return 0;
            }

            // Parse header to find diesel column
            String[] headers = lines[0].split("[,;\t]");
            int dieselCol = -1;
            int countryCol = -1;
            for (int i = 0; i < headers.length; i++) {
                String h = headers[i].toLowerCase().trim();
                if (h.contains("diesel") || h.contains("gas oil")) {
                    dieselCol = i;
                } else if (h.contains("country") || h.contains("member")) {
                    countryCol = i;
                }
            }

            if (dieselCol < 0 || countryCol < 0) {
                log.warn("Could not find diesel/country columns in EU Oil Bulletin CSV. Headers: {}", lines[0]);
                return count;
            }

            for (int i = 1; i < lines.length; i++) {
                String[] cols = lines[i].split("[,;\t]");
                if (cols.length <= Math.max(dieselCol, countryCol)) continue;

                String country = cols[countryCol].trim().toUpperCase();
                // Handle full country names or codes
                String code = resolveCountryCode(country);
                if (code == null || !countryMapping.containsKey(code)) continue;

                String priceStr = cols[dieselCol].trim().replace(",", ".");
                if (priceStr.isEmpty()) continue;

                try {
                    // EU Oil Bulletin prices are in EUR per 1000 liters
                    BigDecimal pricePer1000L = new BigDecimal(priceStr);
                    BigDecimal pricePerLiter = pricePer1000L.divide(new BigDecimal("1000"), 3, RoundingMode.HALF_UP);

                    if (pricePerLiter.compareTo(new BigDecimal("0.5")) >= 0 && pricePerLiter.compareTo(new BigDecimal("5.0")) <= 0) {
                        savePrice(code, "DIESEL", pricePerLiter, "EU Oil Bulletin");
                        count++;
                        log.info("Updated {} DIESEL price: {} EUR/L (EU Oil Bulletin)", code, pricePerLiter);
                    }
                } catch (NumberFormatException e) {
                    // skip
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch European fuel prices from EU Oil Bulletin: {}", e.getMessage());
        }
        return count;
    }

    private String resolveCountryCode(String input) {
        if (input.length() == 2) return input;
        return switch (input) {
            case "GERMANY" -> "DE";
            case "FINLAND" -> "FI";
            case "LATVIA" -> "LV";
            case "LITHUANIA" -> "LT";
            case "POLAND" -> "PL";
            case "SWEDEN" -> "SE";
            case "FRANCE" -> "FR";
            case "NETHERLANDS" -> "NL";
            case "BELGIUM" -> "BE";
            case "AUSTRIA" -> "AT";
            case "ITALY" -> "IT";
            case "SPAIN" -> "ES";
            case "ESTONIA" -> "EE";
            default -> null;
        };
    }

    private void savePrice(String countryCode, String fuelType, BigDecimal price, String source) {
        FuelPrice fuelPrice = FuelPrice.builder()
                .countryCode(countryCode)
                .fuelType(fuelType)
                .pricePerLiter(price)
                .validFrom(LocalDate.now())
                .source(source)
                .build();
        fuelPriceService.updatePrice(fuelPrice);
    }
}
