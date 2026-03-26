import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormArray, FormControl, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { VehicleService } from '../shared/services/vehicle.service';
import { CalculationService } from '../shared/services/calculation.service';
import { RouteService, SavedRoute } from '../shared/services/route.service';
import { Vehicle } from '../shared/models/vehicle.model';
import { CalculationResponse, LegDetail, RouteAlternative, TollBreakdown } from '../shared/models/calculation.model';
import { HttpClient } from '@angular/common/http';
import { FuelOptimizationService, FuelOptimizationResponse } from '../shared/services/fuel-optimization.service';
import { PricingService, PricingSuggestionResponse } from '../shared/services/pricing.service';
import { Observable, of, Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap, catchError } from 'rxjs/operators';

@Component({
  selector: 'app-calculator',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatCardModule, MatFormFieldModule,
    MatInputModule, MatSelectModule, MatButtonModule, MatIconModule,
    MatProgressSpinnerModule, MatExpansionModule, MatSlideToggleModule, MatDialogModule,
    MatAutocompleteModule, TranslateModule
  ],
  template: `
    <div class="calculator-container">
      <h1>{{ 'calculator.title' | translate }}</h1>

      <mat-card class="form-card">
        <mat-card-content>
          @if (savedRoutes.length > 0) {
            <mat-form-field appearance="outline" class="route-select">
              <mat-label>{{ 'calculator.select_route' | translate }}</mat-label>
              <mat-select (selectionChange)="onRouteSelect($event.value)">
                @for (r of savedRoutes; track r.id) {
                  <mat-option [value]="r">{{ r.name }} ({{ r.originAddress }} → {{ r.destinationAddress }})</mat-option>
                }
              </mat-select>
              <mat-icon matPrefix>bookmark</mat-icon>
            </mat-form-field>
          }
          <form [formGroup]="form" (ngSubmit)="calculate()">
            <div class="form-grid">
              <mat-form-field appearance="outline">
                <mat-label>{{ 'calculator.origin' | translate }}</mat-label>
                <input matInput formControlName="originAddress" [placeholder]="'calculator.origin_placeholder' | translate" [matAutocomplete]="originAuto" />
                <mat-icon matPrefix>trip_origin</mat-icon>
                <mat-autocomplete #originAuto="matAutocomplete">
                  @for (opt of originSuggestions$ | async; track opt.placeId) {
                    <mat-option [value]="opt.description">{{ opt.description }}</mat-option>
                  }
                </mat-autocomplete>
              </mat-form-field>

              @for (wp of waypointControls; track $index; let i = $index) {
                <div class="waypoint-row">
                  <mat-form-field appearance="outline" class="waypoint-field">
                    <mat-label>{{ 'calculator.waypoint' | translate }} {{ i + 1 }}</mat-label>
                    <input matInput [formControl]="wp" [placeholder]="'calculator.waypoint_placeholder' | translate" [matAutocomplete]="wpAuto" />
                    <mat-icon matPrefix>more_vert</mat-icon>
                    <mat-autocomplete #wpAuto="matAutocomplete">
                      @for (opt of getWaypointSuggestions(i) | async; track opt.placeId) {
                        <mat-option [value]="opt.description">{{ opt.description }}</mat-option>
                      }
                    </mat-autocomplete>
                  </mat-form-field>
                  <button mat-icon-button color="warn" type="button" (click)="removeWaypoint(i)">
                    <mat-icon>close</mat-icon>
                  </button>
                </div>
              }

              <div class="add-waypoint-row">
                <button mat-stroked-button type="button" (click)="addWaypoint()">
                  <mat-icon>add_location</mat-icon> {{ 'calculator.add_waypoint' | translate }}
                </button>
              </div>

              <mat-form-field appearance="outline">
                <mat-label>{{ 'calculator.destination' | translate }}</mat-label>
                <input matInput formControlName="destinationAddress" [placeholder]="'calculator.destination_placeholder' | translate" [matAutocomplete]="destAuto" />
                <mat-icon matPrefix>place</mat-icon>
                <mat-autocomplete #destAuto="matAutocomplete">
                  @for (opt of destinationSuggestions$ | async; track opt.placeId) {
                    <mat-option [value]="opt.description">{{ opt.description }}</mat-option>
                  }
                </mat-autocomplete>
              </mat-form-field>

              <mat-form-field appearance="outline">
                <mat-label>{{ 'calculator.vehicle' | translate }}</mat-label>
                <mat-select formControlName="vehicleId">
                  @for (v of vehicles; track v.id) {
                    <mat-option [value]="v.id">{{ v.name }} ({{ v.fuelType }})</mat-option>
                  }
                </mat-select>
                <mat-icon matPrefix>local_shipping</mat-icon>
              </mat-form-field>

              <mat-form-field appearance="outline">
                <mat-label>{{ 'calculator.cargo_weight' | translate }}</mat-label>
                <input matInput formControlName="cargoWeightTons" type="number" />
                <mat-icon matPrefix>scale</mat-icon>
              </mat-form-field>

              <div class="price-row">
                <mat-form-field appearance="outline" class="price-field">
                  <mat-label>{{ 'calculator.order_price' | translate }}</mat-label>
                  <input matInput formControlName="orderPrice" type="number" />
                  <mat-icon matPrefix>payments</mat-icon>
                </mat-form-field>
                <mat-form-field appearance="outline" class="currency-field">
                  <mat-label>{{ 'calculator.currency' | translate }}</mat-label>
                  <mat-select formControlName="currency">
                    <mat-option value="EUR">EUR €</mat-option>
                    <mat-option value="USD">USD $</mat-option>
                    <mat-option value="GBP">GBP £</mat-option>
                    <mat-option value="PLN">PLN zł</mat-option>
                    <mat-option value="SEK">SEK kr</mat-option>
                    <mat-option value="CZK">CZK Kč</mat-option>
                    <mat-option value="NOK">NOK kr</mat-option>
                    <mat-option value="DKK">DKK kr</mat-option>
                    <mat-option value="RON">RON lei</mat-option>
                    <mat-option value="HUF">HUF Ft</mat-option>
                    <mat-option value="CHF">CHF Fr</mat-option>
                  </mat-select>
                </mat-form-field>
              </div>

              @if (canGetPriceSuggestion()) {
                <div class="pricing-suggest-row">
                  <button mat-stroked-button color="accent" type="button" (click)="getPriceSuggestion()" [disabled]="pricingLoading">
                    @if (pricingLoading) {
                      <mat-spinner diameter="18"></mat-spinner>
                    } @else {
                      <mat-icon>auto_awesome</mat-icon> {{ 'pricing.get_suggestion' | translate }}
                    }
                  </button>
                </div>
              }

              @if (pricingSuggestion) {
                <div class="pricing-tiles">
                  <div class="pricing-tile pricing-tile-min" (click)="useSuggestedPrice(pricingSuggestion.priceRange.minPrice)">
                    <div class="pricing-tile-label">{{ 'pricing.minimum' | translate }}</div>
                    <div class="pricing-tile-price">{{ pricingSuggestion.priceRange.minPrice | number:'1.0-0' }} EUR</div>
                    <div class="pricing-tile-margin">15% {{ 'pricing.margin' | translate }}</div>
                    <div class="pricing-tile-rev">{{ pricingSuggestion.priceRange.minRevenuePerKm | number:'1.3-3' }} EUR/km</div>
                    <button mat-flat-button color="warn" type="button" class="pricing-tile-btn" (click)="useSuggestedPrice(pricingSuggestion.priceRange.minPrice); $event.stopPropagation()">
                      {{ 'pricing.use_price' | translate }}
                    </button>
                  </div>
                  <div class="pricing-tile pricing-tile-optimal" (click)="useSuggestedPrice(pricingSuggestion.priceRange.optimalPrice)">
                    <div class="pricing-tile-badge">{{ 'pricing.recommended' | translate }}</div>
                    <div class="pricing-tile-label">{{ 'pricing.optimal' | translate }}</div>
                    <div class="pricing-tile-price">{{ pricingSuggestion.priceRange.optimalPrice | number:'1.0-0' }} EUR</div>
                    <div class="pricing-tile-margin">25% {{ 'pricing.margin' | translate }}</div>
                    <div class="pricing-tile-rev">{{ pricingSuggestion.priceRange.optimalRevenuePerKm | number:'1.3-3' }} EUR/km</div>
                    <button mat-flat-button color="primary" type="button" class="pricing-tile-btn" (click)="useSuggestedPrice(pricingSuggestion.priceRange.optimalPrice); $event.stopPropagation()">
                      {{ 'pricing.use_price' | translate }}
                    </button>
                  </div>
                  <div class="pricing-tile pricing-tile-premium" (click)="useSuggestedPrice(pricingSuggestion.priceRange.premiumPrice)">
                    <div class="pricing-tile-label">{{ 'pricing.premium' | translate }}</div>
                    <div class="pricing-tile-price">{{ pricingSuggestion.priceRange.premiumPrice | number:'1.0-0' }} EUR</div>
                    <div class="pricing-tile-margin">35% {{ 'pricing.margin' | translate }}</div>
                    <div class="pricing-tile-rev">{{ pricingSuggestion.priceRange.premiumRevenuePerKm | number:'1.3-3' }} EUR/km</div>
                    <button mat-flat-button color="accent" type="button" class="pricing-tile-btn" (click)="useSuggestedPrice(pricingSuggestion.priceRange.premiumPrice); $event.stopPropagation()">
                      {{ 'pricing.use_price' | translate }}
                    </button>
                  </div>
                </div>
                <div class="pricing-cost-info">
                  {{ 'pricing.estimated_cost' | translate }}: <strong>{{ pricingSuggestion.priceRange.estimatedTotalCost | number:'1.2-2' }} EUR</strong>
                  &nbsp;|&nbsp; {{ 'calculator.distance' | translate }}: <strong>{{ pricingSuggestion.priceRange.distanceKm | number:'1.0-0' }} km</strong>
                  &nbsp;|&nbsp; {{ 'calculator.cost_per_km' | translate }}: <strong>{{ pricingSuggestion.priceRange.costPerKm | number:'1.3-3' }} EUR</strong>
                </div>

                @if (pricingSuggestion.laneIntelligence) {
                  <mat-accordion class="lane-intel-panel">
                    <mat-expansion-panel>
                      <mat-expansion-panel-header>
                        <mat-panel-title>
                          <mat-icon>insights</mat-icon>&nbsp;{{ 'pricing.lane_intelligence' | translate }}
                          ({{ pricingSuggestion.laneIntelligence.tripCount }} {{ 'pricing.trips' | translate }})
                        </mat-panel-title>
                      </mat-expansion-panel-header>
                      <div class="lane-intel-content">
                        <div class="lane-intel-metrics">
                          <div class="lane-metric">
                            <span class="lane-metric-label">{{ 'pricing.avg_price' | translate }}</span>
                            <span class="lane-metric-value">{{ pricingSuggestion.laneIntelligence.avgPrice | number:'1.0-0' }} EUR</span>
                          </div>
                          <div class="lane-metric">
                            <span class="lane-metric-label">{{ 'pricing.avg_margin' | translate }}</span>
                            <span class="lane-metric-value">{{ pricingSuggestion.laneIntelligence.avgMarginPct | number:'1.1-1' }}%</span>
                          </div>
                          <div class="lane-metric">
                            <span class="lane-metric-label">{{ 'pricing.avg_rev_km' | translate }}</span>
                            <span class="lane-metric-value">{{ pricingSuggestion.laneIntelligence.avgRevenuePerKm | number:'1.3-3' }} EUR/km</span>
                          </div>
                          <div class="lane-metric">
                            <span class="lane-metric-label">{{ 'pricing.trend' | translate }}</span>
                            <span class="lane-metric-value">
                              @if (pricingSuggestion.laneIntelligence.priceTrend === 'RISING') {
                                <mat-icon class="trend-up">trending_up</mat-icon>
                              } @else if (pricingSuggestion.laneIntelligence.priceTrend === 'FALLING') {
                                <mat-icon class="trend-down">trending_down</mat-icon>
                              } @else {
                                <mat-icon class="trend-flat">trending_flat</mat-icon>
                              }
                              {{ pricingSuggestion.laneIntelligence.priceTrend }}
                            </span>
                          </div>
                          <div class="lane-metric">
                            <span class="lane-metric-label">{{ 'pricing.best_margin' | translate }}</span>
                            <span class="lane-metric-value">{{ pricingSuggestion.laneIntelligence.bestTripMarginPct | number:'1.1-1' }}%</span>
                          </div>
                          <div class="lane-metric">
                            <span class="lane-metric-label">{{ 'pricing.worst_margin' | translate }}</span>
                            <span class="lane-metric-value">{{ pricingSuggestion.laneIntelligence.worstTripMarginPct | number:'1.1-1' }}%</span>
                          </div>
                        </div>
                        @if (pricingSuggestion.laneIntelligence.matchType === 'FUZZY') {
                          <p class="lane-match-note">{{ 'pricing.fuzzy_match' | translate }}</p>
                        }
                        @if (pricingSuggestion.marketContext) {
                          <div class="market-context">
                            <h4>{{ 'pricing.company_comparison' | translate }}</h4>
                            <div class="lane-intel-metrics">
                              <div class="lane-metric">
                                <span class="lane-metric-label">{{ 'pricing.company_avg_margin' | translate }}</span>
                                <span class="lane-metric-value">{{ pricingSuggestion.marketContext.companyAvgMarginPct | number:'1.1-1' }}%</span>
                              </div>
                              <div class="lane-metric">
                                <span class="lane-metric-label">{{ 'pricing.lane_vs_company' | translate }}</span>
                                <span class="lane-metric-value" [class.positive]="pricingSuggestion.marketContext.laneVsCompanyMarginDelta > 0" [class.negative]="pricingSuggestion.marketContext.laneVsCompanyMarginDelta < 0">
                                  {{ pricingSuggestion.marketContext.laneVsCompanyMarginDelta > 0 ? '+' : '' }}{{ pricingSuggestion.marketContext.laneVsCompanyMarginDelta | number:'1.1-1' }}%
                                </span>
                              </div>
                            </div>
                          </div>
                        }
                      </div>
                    </mat-expansion-panel>
                  </mat-accordion>
                }
              }

              <mat-form-field appearance="outline">
                <mat-label>{{ 'calculator.driver_daily_rate' | translate }}</mat-label>
                <input matInput formControlName="driverDailyRate" type="number" />
                <mat-icon matPrefix>person</mat-icon>
              </mat-form-field>

              <mat-form-field appearance="outline">
                <mat-label>{{ 'calculator.other_costs' | translate }}</mat-label>
                <input matInput formControlName="otherCosts" type="number" />
                <mat-icon matPrefix>receipt</mat-icon>
              </mat-form-field>

              <div class="toggle-field">
                <mat-slide-toggle formControlName="includeReturnTrip" color="primary">
                  {{ 'calculator.return_trip' | translate }}
                </mat-slide-toggle>
              </div>
            </div>

            <div class="actions">
              <button mat-raised-button color="primary" type="submit" [disabled]="form.invalid || loading" class="calc-button">
                @if (loading) {
                  <mat-spinner diameter="20"></mat-spinner>
                } @else {
                  <ng-container><mat-icon>calculate</mat-icon> {{ 'calculator.calculate' | translate }}</ng-container>
                }
              </button>
            </div>
          </form>
        </mat-card-content>
      </mat-card>

      @if (alternatives.length > 0 && !result) {
        <mat-card class="alternatives-card">
          <mat-card-header>
            <mat-card-title>{{ 'calculator_extra.route_alternatives' | translate }}</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <div class="alternatives-grid">
              @for (alt of alternatives; track alt.index) {
                <mat-card class="alt-card" [class.selected]="selectedAlternative === alt.index" (click)="selectedAlternative = alt.index">
                  <mat-card-header>
                    <mat-card-title>{{ alt.label }}</mat-card-title>
                    <mat-card-subtitle>{{ alt.summary }}</mat-card-subtitle>
                  </mat-card-header>
                  <mat-card-content>
                    <div class="alt-metrics">
                      <div><strong>{{ alt.distanceKm | number:'1.0-0' }}</strong> km</div>
                      <div><strong>{{ alt.estimatedHours | number:'1.1-1' }}</strong> h</div>
                      <div><strong>{{ alt.estimatedFuelCost | number:'1.2-2' }}</strong> EUR {{ 'calculator.fuel_cost' | translate }}</div>
                    </div>
                  </mat-card-content>
                </mat-card>
              }
            </div>
            <div class="actions" style="margin-top:16px">
              <button mat-raised-button color="primary" (click)="confirmRoute()" [disabled]="selectedAlternative === null || loading">
                <mat-icon>check</mat-icon> {{ 'calculator_extra.confirm_route' | translate }}
              </button>
              <button mat-stroked-button (click)="alternatives = []; selectedAlternative = null">
                {{ 'vehicles.cancel' | translate }}
              </button>
            </div>
          </mat-card-content>
        </mat-card>
      }

      @if (result) {
        <mat-card class="result-card">
          <mat-card-header>
            <mat-card-title>{{ 'calculator.results' | translate }}</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <div class="result-summary">
              <div class="profit-display" [class.profit-positive]="result.profit >= 0" [class.profit-negative]="result.profit < 0">
                <span class="profit-label">{{ 'calculator.profit' | translate }}</span>
                <span class="profit-value">{{ result.profit | number:'1.2-2' }} {{ result.currency || 'EUR' }}</span>
              </div>
              <div class="metrics">
                <div class="metric">
                  <span class="metric-label">{{ 'calculator.profit_margin' | translate }}</span>
                  <span class="metric-value">{{ result.profitMarginPct | number:'1.1-1' }}%</span>
                </div>
                <div class="metric">
                  <span class="metric-label">{{ 'calculator.revenue_per_km' | translate }}</span>
                  <span class="metric-value">{{ result.revenuePerKm | number:'1.3-3' }} {{ result.currency || 'EUR' }}/km</span>
                </div>
                <div class="metric">
                  <span class="metric-label">{{ 'calculator.distance' | translate }}</span>
                  <span class="metric-value">{{ result.distanceKm | number:'1.0-0' }} km</span>
                </div>
                <div class="metric">
                  <span class="metric-label">{{ 'calculator.estimated_time' | translate }}</span>
                  <span class="metric-value">{{ result.estimatedHours | number:'1.1-1' }} h</span>
                </div>
                @if (result.driverDays > 0) {
                  <div class="metric">
                    <span class="metric-label">{{ 'calculator.driver_days' | translate }}</span>
                    <span class="metric-value">{{ result.driverDays }} {{ 'calculator.days' | translate }}</span>
                  </div>
                }
                <div class="metric">
                  <span class="metric-label">{{ 'calculator.cost_per_km' | translate }}</span>
                  <span class="metric-value">{{ result.costPerKm | number:'1.3-3' }} {{ result.currency || 'EUR' }}/km</span>
                </div>
                @if (result.co2EmissionsKg > 0) {
                  <div class="metric">
                    <span class="metric-label">{{ 'calculator.co2_emissions' | translate }}</span>
                    <span class="metric-value">{{ result.co2EmissionsKg | number:'1.1-1' }} kg CO2</span>
                  </div>
                }
              </div>
            </div>

            @if (result.legs && result.legs.length > 1) {
              <div class="legs-breakdown">
                <h3>{{ 'calculator.route_legs' | translate }}</h3>
                @for (leg of result.legs; track $index; let i = $index) {
                  <div class="leg-row">
                    <span class="leg-label">{{ i + 1 }}. {{ leg.from }} → {{ leg.to }}</span>
                    <span class="leg-value">{{ leg.distanceKm | number:'1.0-0' }} km / {{ leg.estimatedHours | number:'1.1-1' }} h</span>
                  </div>
                }
              </div>
            }

            <mat-accordion>
              <mat-expansion-panel>
                <mat-expansion-panel-header>
                  <mat-panel-title>{{ 'calculator.cost_breakdown' | translate }}</mat-panel-title>
                  <mat-panel-description>{{ 'calculator.total' | translate }}: {{ result.totalCost | number:'1.2-2' }} EUR</mat-panel-description>
                </mat-expansion-panel-header>
                <div class="cost-breakdown">
                  <div class="cost-row">
                    <span>{{ 'calculator.fuel_cost' | translate }}</span>
                    <span>{{ result.fuelCost | number:'1.2-2' }} {{ result.currency || 'EUR' }}</span>
                  </div>
                  <div class="cost-row">
                    <span>{{ 'calculator.toll_cost' | translate }}</span>
                    <span>{{ result.tollCost | number:'1.2-2' }} {{ result.currency || 'EUR' }}</span>
                  </div>
                  @if (result.tollBreakdown && result.tollBreakdown.length > 0) {
                    @for (tb of result.tollBreakdown; track tb.countryCode) {
                      <div class="cost-row sub-row">
                        <span>&nbsp;&nbsp;{{ tb.countryCode }}: {{ tb.distanceKm | number:'1.0-0' }} km</span>
                        <span>{{ tb.cost | number:'1.2-2' }} {{ result.currency || 'EUR' }}</span>
                      </div>
                    }
                  }
                  <div class="cost-row">
                    <span>{{ 'calculator.driver_cost' | translate }}</span>
                    <span>{{ result.driverDailyCost | number:'1.2-2' }} {{ result.currency || 'EUR' }}</span>
                  </div>
                  <div class="cost-row">
                    <span>{{ 'calculator.other_costs_label' | translate }}</span>
                    <span>{{ result.otherCosts | number:'1.2-2' }} {{ result.currency || 'EUR' }}</span>
                  </div>
                  @if (result.maintenanceCost > 0) {
                    <div class="cost-row">
                      <span>{{ 'calculator_extra.maintenance_cost' | translate }}</span>
                      <span>{{ result.maintenanceCost | number:'1.2-2' }} {{ result.currency || 'EUR' }}</span>
                    </div>
                  }
                  @if (result.tireCost > 0) {
                    <div class="cost-row">
                      <span>{{ 'calculator_extra.tire_cost' | translate }}</span>
                      <span>{{ result.tireCost | number:'1.2-2' }} {{ result.currency || 'EUR' }}</span>
                    </div>
                  }
                  @if (result.depreciationCost > 0) {
                    <div class="cost-row">
                      <span>{{ 'calculator_extra.depreciation_cost' | translate }}</span>
                      <span>{{ result.depreciationCost | number:'1.2-2' }} {{ result.currency || 'EUR' }}</span>
                    </div>
                  }
                  @if (result.insuranceCost > 0) {
                    <div class="cost-row">
                      <span>{{ 'calculator_extra.insurance_cost' | translate }}</span>
                      <span>{{ result.insuranceCost | number:'1.2-2' }} {{ result.currency || 'EUR' }}</span>
                    </div>
                  }
                  @if (result.includeReturnTrip) {
                    <div class="cost-row">
                      <span>{{ 'calculator.return_fuel_cost' | translate }}</span>
                      <span>{{ result.returnFuelCost | number:'1.2-2' }} EUR</span>
                    </div>
                  }
                  <div class="cost-row total">
                    <span>{{ 'calculator.total' | translate }}</span>
                    <span>{{ result.totalCost | number:'1.2-2' }} {{ result.currency || 'EUR' }}</span>
                  </div>
                </div>
              </mat-expansion-panel>
            </mat-accordion>

            <div class="actions result-actions">
              <span class="saved-badge"><mat-icon>check_circle</mat-icon> {{ 'calculator.saved_to_history' | translate }}</span>
              <button mat-stroked-button color="primary" (click)="saveRoute()">
                <mat-icon>bookmark_add</mat-icon> {{ 'calculator.save_route' | translate }}
              </button>
              <button mat-stroked-button color="accent" (click)="downloadPdf()">
                <mat-icon>picture_as_pdf</mat-icon> {{ 'calculator.download_pdf' | translate }}
              </button>
              <button mat-stroked-button color="accent" (click)="downloadExcel()">
                <mat-icon>table_chart</mat-icon> {{ 'calculator.download_excel' | translate }}
              </button>
              <button mat-stroked-button color="primary" (click)="optimizeFuel()">
                <mat-icon>local_gas_station</mat-icon> {{ 'calculator_extra.optimize_fuel' | translate }}
              </button>
            </div>

            @if (fuelOptResult) {
              <mat-accordion class="fuel-optimization-panel">
                <mat-expansion-panel [expanded]="true">
                  <mat-expansion-panel-header>
                    <mat-panel-title>{{ 'calculator_extra.fuel_optimization' | translate }}</mat-panel-title>
                  </mat-expansion-panel-header>

                  @if (fuelOptResult.stops.length > 0) {
                    <table class="fuel-stops-table">
                      <thead>
                        <tr>
                          <th>{{ 'calculator_extra.country' | translate }}</th>
                          <th>{{ 'calculator_extra.fuel_price' | translate }}</th>
                          <th>{{ 'calculator_extra.liters' | translate }}</th>
                          <th>{{ 'calculator_extra.cost' | translate }}</th>
                        </tr>
                      </thead>
                      <tbody>
                        @for (stop of fuelOptResult.stops; track $index) {
                          <tr>
                            <td>{{ stop.countryName }}</td>
                            <td>{{ stop.fuelPricePerLiter | number:'1.3-3' }} EUR/l</td>
                            <td>{{ stop.litersToFill | number:'1.0-0' }} l</td>
                            <td>{{ stop.cost | number:'1.2-2' }} EUR</td>
                          </tr>
                        }
                      </tbody>
                    </table>

                    <div class="savings-summary">
                      <div class="savings-item">
                        <span class="savings-label">{{ 'calculator_extra.naive_cost' | translate }}</span>
                        <span class="savings-value">{{ fuelOptResult.totalNaiveCost | number:'1.2-2' }} EUR</span>
                      </div>
                      <div class="savings-item">
                        <span class="savings-label">{{ 'calculator_extra.optimized_cost' | translate }}</span>
                        <span class="savings-value">{{ fuelOptResult.totalOptimizedCost | number:'1.2-2' }} EUR</span>
                      </div>
                      <div class="savings-item">
                        <span class="savings-label">{{ 'calculator_extra.savings' | translate }}</span>
                        <span class="savings-value">{{ fuelOptResult.savings | number:'1.2-2' }} EUR ({{ fuelOptResult.savingsPercent }}%)</span>
                      </div>
                    </div>
                  } @else {
                    <p>{{ 'dashboard.no_data' | translate }}</p>
                  }
                </mat-expansion-panel>
              </mat-accordion>
            }
          </mat-card-content>
        </mat-card>
      }

      @if (errorMessage) {
        <mat-card class="error-card">
          <mat-card-content>
            <p class="error-message">{{ errorMessage }}</p>
          </mat-card-content>
        </mat-card>
      }
    </div>
  `,
  styleUrl: './calculator.component.scss'
})
export class CalculatorComponent implements OnInit {
  private fb = inject(FormBuilder);
  private vehicleService = inject(VehicleService);
  private calculationService = inject(CalculationService);
  private routeService = inject(RouteService);
  private http = inject(HttpClient);
  private translate = inject(TranslateService);

  vehicles: Vehicle[] = [];
  savedRoutes: SavedRoute[] = [];
  result: CalculationResponse | null = null;
  loading = false;
  saved = false;
  errorMessage = '';
  alternatives: RouteAlternative[] = [];
  selectedAlternative: number | null = null;
  fuelOptResult: FuelOptimizationResponse | null = null;
  private fuelOptService = inject(FuelOptimizationService);
  private pricingService = inject(PricingService);

  pricingSuggestion: PricingSuggestionResponse | null = null;
  pricingLoading = false;

  originSuggestions$!: Observable<{description: string; placeId: string}[]>;
  destinationSuggestions$!: Observable<{description: string; placeId: string}[]>;
  private waypointSuggestions = new Map<number, Observable<{description: string; placeId: string}[]>>();

  form = this.fb.group({
    originAddress: ['', Validators.required],
    destinationAddress: ['', Validators.required],
    waypoints: this.fb.array([] as any[]),
    vehicleId: ['', Validators.required],
    cargoWeightTons: [null as number | null],
    orderPrice: [null as number | null, [Validators.required]],
    currency: ['EUR'],
    driverDailyRate: [null as number | null],
    otherCosts: [null as number | null],
    includeReturnTrip: [false]
  });

  get waypoints(): FormArray {
    return this.form.get('waypoints') as FormArray;
  }

  get waypointControls(): FormControl[] {
    return this.waypoints.controls as FormControl[];
  }

  addWaypoint(): void {
    this.waypoints.push(this.fb.control('', Validators.required));
    this.waypointSuggestions.clear();
  }

  removeWaypoint(index: number): void {
    this.waypoints.removeAt(index);
    this.waypointSuggestions.clear();
  }

  ngOnInit(): void {
    this.vehicleService.getVehicles().subscribe({
      next: (v) => this.vehicles = v.filter(x => x.active),
      error: () => this.errorMessage = this.translate.instant('calculator.vehicles_load_error')
    });
    this.loadRoutes();

    this.originSuggestions$ = this.form.get('originAddress')!.valueChanges.pipe(
      debounceTime(300), distinctUntilChanged(),
      switchMap(val => val && val.length >= 2 ? this.fetchPlaces(val) : of([]))
    );
    this.destinationSuggestions$ = this.form.get('destinationAddress')!.valueChanges.pipe(
      debounceTime(300), distinctUntilChanged(),
      switchMap(val => val && val.length >= 2 ? this.fetchPlaces(val) : of([]))
    );
  }

  private fetchPlaces(input: string): Observable<{description: string; placeId: string}[]> {
    return this.http.get<{description: string; placeId: string}[]>('/api/places/autocomplete', { params: { input } }).pipe(
      catchError(() => of([]))
    );
  }

  getWaypointSuggestions(index: number): Observable<{description: string; placeId: string}[]> {
    if (!this.waypointSuggestions.has(index)) {
      const ctrl = this.waypointControls[index];
      if (!ctrl) return of([]);
      this.waypointSuggestions.set(index, ctrl.valueChanges.pipe(
        debounceTime(300), distinctUntilChanged(),
        switchMap(val => val && val.length >= 2 ? this.fetchPlaces(val) : of([]))
      ));
    }
    return this.waypointSuggestions.get(index)!;
  }

  loadRoutes(): void {
    this.routeService.getRoutes().subscribe(r => this.savedRoutes = r);
  }

  onRouteSelect(route: SavedRoute): void {
    this.form.patchValue({
      originAddress: route.originAddress,
      destinationAddress: route.destinationAddress
    });
  }

  saveRoute(): void {
    const name = prompt(this.translate.instant('calculator.route_name'));
    if (!name) return;
    this.routeService.saveRoute({
      name,
      originAddress: this.form.value.originAddress!,
      destinationAddress: this.form.value.destinationAddress!
    }).subscribe(() => this.loadRoutes());
  }

  downloadPdf(): void {
    if (!this.result) return;
    this.http.get(`/api/calculations/${this.result.id}/pdf`, { responseType: 'blob' }).subscribe(blob => {
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `calculation-${this.result!.id}.pdf`;
      a.click();
      window.URL.revokeObjectURL(url);
    });
  }

  downloadExcel(): void {
    if (!this.result) return;
    this.http.get(`/api/calculations/${this.result.id}/excel`, { responseType: 'blob' }).subscribe(blob => {
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `calculation-${this.result!.id}.xlsx`;
      a.click();
      window.URL.revokeObjectURL(url);
    });
  }

  calculate(): void {
    if (this.form.invalid) return;
    this.loading = true;
    this.result = null;
    this.saved = false;
    this.errorMessage = '';
    this.alternatives = [];
    this.selectedAlternative = null;
    const formVal = this.form.value;
    const request = {
      ...formVal,
      waypoints: this.waypoints.length > 0 ? this.waypoints.value : undefined
    };

    // If no waypoints, try to get alternative routes first
    if (!request.waypoints || request.waypoints.length === 0) {
      this.calculationService.previewAlternatives(request as any).subscribe({
        next: (res) => {
          this.loading = false;
          if (res.alternatives && res.alternatives.length > 1) {
            this.alternatives = res.alternatives;
            this.selectedAlternative = 0;
          } else {
            // Only one route available, calculate directly
            this.doCalculate(request);
          }
        },
        error: () => {
          // Fallback to direct calculation
          this.doCalculate(request);
        }
      });
    } else {
      this.doCalculate(request);
    }
  }

  private doCalculate(request: any): void {
    this.loading = true;
    this.calculationService.calculate(request as any).subscribe({
      next: (res) => {
        this.result = res;
        this.loading = false;
        this.saved = true;
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err.error?.message || this.translate.instant('calculator.calc_error');
      }
    });
  }

  confirmRoute(): void {
    if (this.selectedAlternative === null) return;
    const alt = this.alternatives[this.selectedAlternative];
    const formVal = this.form.value;
    const request = {
      ...formVal,
      waypoints: this.waypoints.length > 0 ? this.waypoints.value : undefined
    };
    this.alternatives = [];
    this.doCalculate(request);
  }

  optimizeFuel(): void {
    if (!this.result) return;
    this.fuelOptService.optimize({
      vehicleId: this.form.value.vehicleId!,
      originAddress: this.form.value.originAddress!,
      destinationAddress: this.form.value.destinationAddress!,
      distanceKm: this.result.distanceKm,
      isLoaded: true
    }).subscribe({
      next: (res) => this.fuelOptResult = res,
      error: () => this.fuelOptResult = null
    });
  }

  canGetPriceSuggestion(): boolean {
    const v = this.form.value;
    return !!(v.originAddress && v.destinationAddress && v.vehicleId && !v.orderPrice);
  }

  getPriceSuggestion(): void {
    const v = this.form.value;
    if (!v.originAddress || !v.destinationAddress || !v.vehicleId) return;
    this.pricingLoading = true;
    this.pricingSuggestion = null;
    this.pricingService.suggest({
      originAddress: v.originAddress,
      destinationAddress: v.destinationAddress,
      vehicleId: v.vehicleId,
      cargoWeightTons: v.cargoWeightTons ?? undefined,
      includeReturnTrip: v.includeReturnTrip ?? false
    }).subscribe({
      next: (res) => {
        this.pricingSuggestion = res;
        this.pricingLoading = false;
      },
      error: () => {
        this.pricingLoading = false;
        this.errorMessage = this.translate.instant('pricing.error');
      }
    });
  }

  useSuggestedPrice(price: number): void {
    this.form.patchValue({ orderPrice: Math.round(price) });
    this.pricingSuggestion = null;
  }

  save(): void {
    this.saved = true;
  }
}
