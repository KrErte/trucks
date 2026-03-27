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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "fuel.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class FuelPriceScheduler {

    private static final Logger log = LoggerFactory.getLogger(FuelPriceScheduler.class);

    private final FuelPriceService fuelPriceService;

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

        // Country code -> fuel-prices.eu URL path
        Map<String, String> countries = new LinkedHashMap<>();
        countries.put("LV", "Latvia");
        countries.put("LT", "Lithuania");
        countries.put("DE", "Germany");
        countries.put("FI", "Finland");
        countries.put("PL", "Poland");
        countries.put("SE", "Sweden");
        countries.put("FR", "France");
        countries.put("NL", "Netherlands");
        countries.put("BE", "Belgium");
        countries.put("AT", "Austria");
        countries.put("IT", "Italy");
        countries.put("ES", "Spain");

        for (Map.Entry<String, String> entry : countries.entrySet()) {
            try {
                // Rate limit: small delay between requests
                Thread.sleep(500);

                String url = "https://www.fuel-prices.eu/" + entry.getValue() + "/";
                Document doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (FuelFleet Bot)")
                        .timeout(15000)
                        .get();

                // fuel-prices.eu shows prices like "€1.234" or "€ 1.234" on country pages
                String bodyText = doc.body().text();

                // Look for diesel price pattern
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                        "(?i)diesel[^€]*€\\s*(\\d+[.,]\\d{2,3})"
                );
                java.util.regex.Matcher matcher = pattern.matcher(bodyText);
                if (matcher.find()) {
                    String priceStr = matcher.group(1).replace(",", ".");
                    BigDecimal price = new BigDecimal(priceStr);
                    if (price.compareTo(new BigDecimal("0.5")) >= 0 && price.compareTo(new BigDecimal("5.0")) <= 0) {
                        savePrice(entry.getKey(), "DIESEL", price, "fuel-prices.eu");
                        count++;
                        log.info("Updated {} DIESEL price: {} EUR/L (fuel-prices.eu)", entry.getKey(), price);
                    }
                } else {
                    // Fallback: try to find any price-like pattern near "diesel"
                    java.util.regex.Pattern fallback = java.util.regex.Pattern.compile(
                            "(?i)diesel[\\s\\S]{0,100}?(\\d+[.,]\\d{2,3})\\s*(?:€|EUR|eur)"
                    );
                    java.util.regex.Matcher fbMatcher = fallback.matcher(bodyText);
                    if (fbMatcher.find()) {
                        String priceStr = fbMatcher.group(1).replace(",", ".");
                        BigDecimal price = new BigDecimal(priceStr);
                        if (price.compareTo(new BigDecimal("0.5")) >= 0 && price.compareTo(new BigDecimal("5.0")) <= 0) {
                            savePrice(entry.getKey(), "DIESEL", price, "fuel-prices.eu");
                            count++;
                            log.info("Updated {} DIESEL price: {} EUR/L (fuel-prices.eu fallback)", entry.getKey(), price);
                        }
                    } else {
                        log.warn("Could not parse diesel price for {} from fuel-prices.eu", entry.getKey());
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Failed to fetch fuel price for {}: {}", entry.getKey(), e.getMessage());
            }
        }

        return count;
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
