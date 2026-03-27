package eu.fuelfleet.scraper.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.fuelfleet.scraper.dto.ScrapedTruckData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class Truck1ScraperService {

    private static final String BASE_URL = "https://www.truck1.eu";
    private static final String SEARCH_URL = BASE_URL + "/trucks/%s/%s/a-7ct--1.html";
    private static final int TIMEOUT_MS = 15000;

    private final ObjectMapper objectMapper;

    private long lastRequestTime = 0;

    public List<ScrapedTruckData> searchTrucks(String brand, String type, int page) {
        rateLimitWait();

        String category = mapType(type);
        String url = String.format(SEARCH_URL, category, brand.toLowerCase());
        if (page > 1) {
            url = url.replace(".html", "-p-" + page + ".html");
        }

        log.info("Scraping Truck1 search: {}", url);

        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(TIMEOUT_MS)
                    .get();

            return parseSearchResults(doc);
        } catch (Exception e) {
            log.error("Failed to scrape Truck1 search", e);
            return List.of();
        }
    }

    public ScrapedTruckData getTruckDetails(String listingUrl) {
        rateLimitWait();

        log.info("Scraping Truck1 details: {}", listingUrl);

        try {
            Document doc = Jsoup.connect(listingUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(TIMEOUT_MS)
                    .get();

            return parseDetailPage(doc, listingUrl);
        } catch (Exception e) {
            log.error("Failed to scrape Truck1 details: {}", listingUrl, e);
            return null;
        }
    }

    private List<ScrapedTruckData> parseSearchResults(Document doc) {
        List<ScrapedTruckData> results = new ArrayList<>();

        // Try JSON-LD first
        Elements jsonLdScripts = doc.select("script[type=application/ld+json]");
        for (Element script : jsonLdScripts) {
            try {
                JsonNode json = objectMapper.readTree(script.data());
                if (json.has("@type") && "ItemList".equals(json.get("@type").asText())) {
                    JsonNode items = json.get("itemListElement");
                    if (items != null && items.isArray()) {
                        for (JsonNode item : items) {
                            JsonNode product = item.has("item") ? item.get("item") : item;
                            ScrapedTruckData truck = parseJsonLdProduct(product);
                            if (truck != null) {
                                results.add(truck);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("Failed to parse JSON-LD", e);
            }
        }

        // Fallback to HTML parsing if JSON-LD didn't yield results
        if (results.isEmpty()) {
            Elements listings = doc.select("div.offer-card, div.search-result-item, article.offer");
            for (Element listing : listings) {
                try {
                    ScrapedTruckData truck = parseHtmlListing(listing);
                    if (truck != null) {
                        results.add(truck);
                    }
                } catch (Exception e) {
                    log.debug("Failed to parse listing", e);
                }
            }
        }

        return results;
    }

    private ScrapedTruckData parseJsonLdProduct(JsonNode product) {
        if (product == null) return null;

        ScrapedTruckData.ScrapedTruckDataBuilder builder = ScrapedTruckData.builder();

        builder.name(getJsonText(product, "name"));
        builder.sourceUrl(getJsonText(product, "url"));

        // Extract source ID from URL
        String url = getJsonText(product, "url");
        if (url != null) {
            String[] parts = url.split("/");
            if (parts.length > 0) {
                String lastPart = parts[parts.length - 1].replace(".html", "");
                builder.sourceId(lastPart);
            }
        }

        // Brand
        if (product.has("brand")) {
            JsonNode brandNode = product.get("brand");
            builder.brand(brandNode.isObject() ? getJsonText(brandNode, "name") : brandNode.asText());
        }

        // Price
        if (product.has("offers")) {
            JsonNode offers = product.get("offers");
            if (offers.isArray() && offers.size() > 0) {
                offers = offers.get(0);
            }
            if (offers.has("price")) {
                try {
                    builder.price(new BigDecimal(offers.get("price").asText()));
                } catch (NumberFormatException ignored) {}
            }
            builder.currency(getJsonText(offers, "priceCurrency"));
        }

        // Image
        if (product.has("image")) {
            JsonNode img = product.get("image");
            if (img.isArray() && img.size() > 0) {
                builder.imageUrl(img.get(0).asText());
            } else if (img.isTextual()) {
                builder.imageUrl(img.asText());
            }
        }

        return builder.build();
    }

    private ScrapedTruckData parseHtmlListing(Element listing) {
        ScrapedTruckData.ScrapedTruckDataBuilder builder = ScrapedTruckData.builder();

        Element titleLink = listing.selectFirst("a[href*=/trucks/]");
        if (titleLink == null) return null;

        String href = titleLink.attr("abs:href");
        builder.sourceUrl(href);
        builder.name(titleLink.text().trim());

        String[] parts = href.split("/");
        if (parts.length > 0) {
            builder.sourceId(parts[parts.length - 1].replace(".html", ""));
        }

        Element priceEl = listing.selectFirst(".price, .offer-price, [class*=price]");
        if (priceEl != null) {
            String priceText = priceEl.text().replaceAll("[^0-9.]", "");
            try {
                builder.price(new BigDecimal(priceText));
            } catch (NumberFormatException ignored) {}
        }

        Element imgEl = listing.selectFirst("img[src]");
        if (imgEl != null) {
            builder.imageUrl(imgEl.attr("abs:src"));
        }

        Element locationEl = listing.selectFirst(".location, [class*=location]");
        if (locationEl != null) {
            builder.location(locationEl.text().trim());
        }

        return builder.build();
    }

    private ScrapedTruckData parseDetailPage(Document doc, String url) {
        ScrapedTruckData.ScrapedTruckDataBuilder builder = ScrapedTruckData.builder();
        builder.sourceUrl(url);

        // Parse JSON-LD from detail page
        Elements jsonLdScripts = doc.select("script[type=application/ld+json]");
        for (Element script : jsonLdScripts) {
            try {
                JsonNode json = objectMapper.readTree(script.data());
                if (json.has("@type") && "Product".equals(json.get("@type").asText())) {
                    ScrapedTruckData fromJson = parseJsonLdProduct(json);
                    if (fromJson != null) {
                        builder.name(fromJson.getName());
                        builder.brand(fromJson.getBrand());
                        builder.price(fromJson.getPrice());
                        builder.currency(fromJson.getCurrency());
                        builder.imageUrl(fromJson.getImageUrl());
                    }
                }
            } catch (Exception e) {
                log.debug("Failed to parse detail JSON-LD", e);
            }
        }

        // Extract source ID from URL
        String[] urlParts = url.split("/");
        if (urlParts.length > 0) {
            builder.sourceId(urlParts[urlParts.length - 1].replace(".html", ""));
        }

        // Parse spec table from HTML
        Elements specRows = doc.select("tr, .param-row, .spec-row, dl dt, .detail-row");
        for (Element row : specRows) {
            String label = "";
            String value = "";

            Element labelEl = row.selectFirst("td:first-child, th, dt, .param-label, .spec-label");
            Element valueEl = row.selectFirst("td:last-child, dd, .param-value, .spec-value");

            if (labelEl != null && valueEl != null) {
                label = labelEl.text().trim().toLowerCase();
                value = valueEl.text().trim();
            }

            if (label.isEmpty() || value.isEmpty()) continue;

            if (label.contains("axle") || label.contains("teljevalem") || label.contains("wheel formula")) {
                builder.axleConfiguration(value);
                try {
                    int firstDigit = Character.getNumericValue(value.charAt(0));
                    builder.numberOfAxles(firstDigit / 2);
                } catch (Exception ignored) {}
            } else if (label.contains("gross weight") || label.contains("täismass") || label.contains("gvw")) {
                try {
                    builder.grossWeight(new BigDecimal(value.replaceAll("[^0-9.]", "")));
                } catch (NumberFormatException ignored) {}
            } else if (label.contains("net weight") || label.contains("omamass") || label.contains("curb")) {
                try {
                    builder.netWeight(new BigDecimal(value.replaceAll("[^0-9.]", "")));
                } catch (NumberFormatException ignored) {}
            } else if (label.contains("power") || label.contains("võimsus") || label.contains("hp") || label.contains("horse")) {
                try {
                    builder.powerHp(Integer.parseInt(value.replaceAll("[^0-9]", "")));
                } catch (NumberFormatException ignored) {}
            } else if (label.contains("displacement") || label.contains("töömaht") || label.contains("engine capacity")) {
                try {
                    builder.displacementCc(Integer.parseInt(value.replaceAll("[^0-9]", "")));
                } catch (NumberFormatException ignored) {}
            } else if (label.contains("gearbox") || label.contains("käigukast") || label.contains("transmission")) {
                builder.gearbox(value.toLowerCase().contains("auto") ? "automatic" : "manual");
            } else if (label.contains("suspension") || label.contains("vedrustus")) {
                builder.suspension(value.toLowerCase());
            } else if (label.contains("tank") || label.contains("kütusepaak") || label.contains("fuel tank")) {
                try {
                    builder.tankCapacity(new BigDecimal(value.replaceAll("[^0-9.]", "")));
                } catch (NumberFormatException ignored) {}
            } else if (label.contains("mileage") || label.contains("läbisõit") || label.contains("odometer")) {
                try {
                    builder.mileageKm(Integer.parseInt(value.replaceAll("[^0-9]", "")));
                } catch (NumberFormatException ignored) {}
            } else if (label.contains("euro") || label.contains("emission")) {
                builder.euroClass(normalizeEuroClass(value));
            } else if (label.contains("fuel") || label.contains("kütus")) {
                builder.fuelType(normalizeFuelType(value));
            } else if (label.contains("year") || label.contains("aasta")) {
                try {
                    builder.year(Integer.parseInt(value.replaceAll("[^0-9]", "").substring(0, 4)));
                } catch (Exception ignored) {}
            } else if (label.contains("location") || label.contains("asukoht")) {
                builder.location(value);
            }
        }

        // Fallback: parse title for brand/model
        Element titleEl = doc.selectFirst("h1");
        if (titleEl != null) {
            String title = titleEl.text().trim();
            if (builder.build().getName() == null) {
                builder.name(title);
            }
        }

        return builder.build();
    }

    private String normalizeEuroClass(String value) {
        value = value.toUpperCase().replaceAll("[^0-9A-Z]", "");
        if (value.contains("6") || value.contains("VI")) return "EURO_6";
        if (value.contains("5") || value.contains("V")) return "EURO_5";
        if (value.contains("4") || value.contains("IV")) return "EURO_4";
        if (value.contains("3") || value.contains("III")) return "EURO_3";
        return value;
    }

    private String normalizeFuelType(String value) {
        value = value.toUpperCase();
        if (value.contains("DIESEL")) return "DIESEL";
        if (value.contains("PETROL") || value.contains("GASOLINE") || value.contains("BENSIIN")) return "PETROL";
        if (value.contains("LNG")) return "LNG";
        if (value.contains("CNG")) return "CNG";
        return "DIESEL";
    }

    private String getJsonText(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : null;
    }

    private String mapType(String type) {
        if (type == null) return "tractor-units";
        return switch (type.toLowerCase()) {
            case "tractor", "tractor-units", "sadulveokid" -> "tractor-units";
            case "rigid", "rigid-trucks", "veoautod" -> "rigid-trucks";
            case "tipper", "tippers", "kallurid" -> "tippers";
            case "curtain", "curtainsider" -> "curtainsider-trucks";
            default -> type.toLowerCase();
        };
    }

    private synchronized void rateLimitWait() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastRequestTime;
        if (elapsed < 1000) {
            try {
                TimeUnit.MILLISECONDS.sleep(1000 - elapsed);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        lastRequestTime = System.currentTimeMillis();
    }
}
