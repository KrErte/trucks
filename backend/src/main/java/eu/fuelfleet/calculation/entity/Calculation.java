package eu.fuelfleet.calculation.entity;

import eu.fuelfleet.auth.entity.User;
import eu.fuelfleet.company.entity.Company;
import eu.fuelfleet.vehicle.entity.Vehicle;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "calculations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Calculation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String origin;

    @Column(nullable = false)
    private String destination;

    @Column(name = "distance_km")
    private BigDecimal distanceKm;

    @Column(name = "estimated_hours")
    private BigDecimal estimatedHours;

    @Column(name = "cargo_weight_t")
    private BigDecimal cargoWeightT;

    @Column(name = "order_price")
    private BigDecimal orderPrice;

    private String currency;

    @Column(name = "fuel_cost")
    private BigDecimal fuelCost;

    @Column(name = "toll_cost")
    private BigDecimal tollCost;

    @Column(name = "driver_daily_cost")
    private BigDecimal driverDailyCost;

    @Column(name = "other_costs")
    private BigDecimal otherCosts;

    @Column(name = "total_cost")
    private BigDecimal totalCost;

    private BigDecimal profit;

    @Column(name = "profit_margin_pct")
    private BigDecimal profitMarginPct;

    @Column(name = "revenue_per_km")
    private BigDecimal revenuePerKm;

    @Column(name = "include_return_trip")
    private boolean includeReturnTrip;

    @Column(name = "return_fuel_cost")
    private BigDecimal returnFuelCost;

    @Column(columnDefinition = "text")
    private String waypoints;

    @Column(name = "fuel_breakdown", columnDefinition = "jsonb")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private String fuelBreakdown;

    @Column(name = "co2_emissions_kg")
    private BigDecimal co2EmissionsKg;

    @Column(name = "driver_days")
    private Integer driverDays;

    @Column(name = "maintenance_cost")
    private BigDecimal maintenanceCost;

    @Column(name = "tire_cost")
    private BigDecimal tireCost;

    @Column(name = "depreciation_cost")
    private BigDecimal depreciationCost;

    @Column(name = "insurance_cost")
    private BigDecimal insuranceCost;

    @Column(name = "toll_breakdown", columnDefinition = "jsonb")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private String tollBreakdown;

    @Column(name = "route_index")
    private Integer routeIndex;

    @Column(name = "route_label")
    private String routeLabel;

    @Column(name = "route_alternatives", columnDefinition = "jsonb")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private String routeAlternatives;

    @Column(name = "cost_per_km")
    private BigDecimal costPerKm;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
