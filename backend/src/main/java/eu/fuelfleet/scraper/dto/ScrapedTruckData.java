package eu.fuelfleet.scraper.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScrapedTruckData {
    private String sourceId;
    private String sourceUrl;
    private String name;
    private String brand;
    private String model;
    private Integer year;
    private String fuelType;
    private String euroClass;
    private BigDecimal price;
    private String currency;
    private String axleConfiguration;
    private Integer numberOfAxles;
    private BigDecimal grossWeight;
    private BigDecimal netWeight;
    private Integer powerHp;
    private Integer displacementCc;
    private String gearbox;
    private String suspension;
    private BigDecimal tankCapacity;
    private Integer mileageKm;
    private String imageUrl;
    private String location;
}
