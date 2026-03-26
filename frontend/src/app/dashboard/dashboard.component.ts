import { Component, inject, OnInit, ElementRef, ViewChild, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { TranslateModule } from '@ngx-translate/core';

interface MonthlyData {
  month: string;
  profit: number;
  tripCount: number;
  avgMargin: number;
}

interface VehicleStats {
  vehicleId: string;
  vehicleName: string;
  tripCount: number;
  totalProfit: number;
  avgMargin: number;
  totalDistanceKm: number;
}

interface DashboardResponse {
  totalProfit: number;
  tripCount: number;
  avgMargin: number;
  monthlyData: MonthlyData[];
  vehicleStats: VehicleStats[];
}

interface CalculationRecord {
  originAddress: string;
  destinationAddress: string;
  orderPrice: number;
  totalCost: number;
  profit: number;
  profitMarginPct: number;
  fuelCost: number;
  tollCost: number;
  driverDailyCost: number;
  otherCosts: number;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatIconModule, TranslateModule],
  template: `
    <div class="dashboard-container">
      <h1>{{ 'dashboard.title' | translate }}</h1>

      <div class="stats-grid">
        <mat-card class="stat-card profit-card">
          <div class="stat-icon-wrap">
            <mat-icon>account_balance_wallet</mat-icon>
          </div>
          <div class="stat-value" [class.positive]="data && data.totalProfit >= 0" [class.negative]="data && data.totalProfit < 0">
            {{ data?.totalProfit | number:'1.2-2' }} €
          </div>
          <div class="stat-label">{{ 'dashboard.total_profit' | translate }}</div>
        </mat-card>

        <mat-card class="stat-card trips-card">
          <div class="stat-icon-wrap">
            <mat-icon>local_shipping</mat-icon>
          </div>
          <div class="stat-value">{{ data?.tripCount }}</div>
          <div class="stat-label">{{ 'dashboard.trip_count' | translate }}</div>
        </mat-card>

        <mat-card class="stat-card margin-card">
          <div class="stat-icon-wrap">
            <mat-icon>trending_up</mat-icon>
          </div>
          <div class="stat-value">{{ data?.avgMargin | number:'1.1-1' }}%</div>
          <div class="stat-label">{{ 'dashboard.avg_margin' | translate }}</div>
        </mat-card>
      </div>

      <div class="charts-row">
        @if (data && data.monthlyData.length > 0) {
          <mat-card class="chart-card">
            <h2>{{ 'dashboard.monthly_overview' | translate }}</h2>
            <div class="bar-chart">
              @for (m of data.monthlyData; track m.month) {
                <div class="bar-col">
                  <div class="bar-value">{{ m.profit | number:'1.0-0' }}€</div>
                  <div class="bar-track">
                    <div class="bar-fill"
                      [class.positive]="m.profit >= 0"
                      [class.negative]="m.profit < 0"
                      [style.height.%]="getBarHeight(m.profit)">
                    </div>
                  </div>
                  <div class="bar-label">{{ formatMonth(m.month) }}</div>
                  <div class="bar-sub">{{ m.tripCount }} {{ 'dashboard.trips' | translate }}</div>
                </div>
              }
            </div>
          </mat-card>
        }

        @if (costBreakdown) {
          <mat-card class="chart-card">
            <h2>{{ 'dashboard.cost_breakdown' | translate }}</h2>
            <div class="donut-chart">
              <svg viewBox="0 0 200 200" class="donut-svg">
                @for (seg of donutSegments; track seg.label) {
                  <circle class="donut-segment"
                    cx="100" cy="100" r="70"
                    [attr.stroke]="seg.color"
                    [attr.stroke-dasharray]="seg.dashArray"
                    [attr.stroke-dashoffset]="seg.dashOffset"
                    fill="none"
                    stroke-width="30" />
                }
                <text x="100" y="95" text-anchor="middle" class="donut-total-value">
                  {{ costBreakdown.total | number:'1.0-0' }}€
                </text>
                <text x="100" y="115" text-anchor="middle" class="donut-total-label">
                  {{ 'dashboard.total_costs' | translate }}
                </text>
              </svg>
              <div class="donut-legend">
                @for (seg of donutSegments; track seg.label) {
                  <div class="legend-item">
                    <span class="legend-dot" [style.background]="seg.color"></span>
                    <span class="legend-label">{{ seg.label | translate }}</span>
                    <span class="legend-value">{{ seg.value | number:'1.0-0' }}€</span>
                    <span class="legend-pct">{{ seg.pct | number:'1.0-0' }}%</span>
                  </div>
                }
              </div>
            </div>
          </mat-card>
        }
      </div>

      @if (data && data.vehicleStats && data.vehicleStats.length > 0) {
        <mat-card class="chart-card vehicle-stats-card">
          <h2>{{ 'dashboard.vehicle_stats' | translate }}</h2>
          <table class="vehicle-table">
            <thead>
              <tr>
                <th>{{ 'dashboard.vehicle_name' | translate }}</th>
                <th>{{ 'dashboard.trips' | translate }}</th>
                <th>{{ 'dashboard.total_distance' | translate }}</th>
                <th>{{ 'dashboard.profit' | translate }}</th>
                <th>{{ 'dashboard.avg_margin' | translate }}</th>
              </tr>
            </thead>
            <tbody>
              @for (v of data.vehicleStats; track v.vehicleId) {
                <tr>
                  <td>{{ v.vehicleName }}</td>
                  <td>{{ v.tripCount }}</td>
                  <td>{{ v.totalDistanceKm | number:'1.0-0' }} km</td>
                  <td [class.positive]="v.totalProfit >= 0" [class.negative]="v.totalProfit < 0">
                    {{ v.totalProfit | number:'1.2-2' }} €
                  </td>
                  <td>{{ v.avgMargin | number:'1.1-1' }}%</td>
                </tr>
              }
            </tbody>
          </table>
        </mat-card>
      }

      @if (!data || data.monthlyData.length === 0) {
        <mat-card class="no-data-card">
          <mat-icon>info</mat-icon>
          <p>{{ 'dashboard.no_data' | translate }}</p>
        </mat-card>
      }
    </div>
  `,
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit {
  private http = inject(HttpClient);
  data: DashboardResponse | null = null;
  costBreakdown: { fuel: number; toll: number; driver: number; other: number; total: number } | null = null;
  donutSegments: { label: string; value: number; pct: number; color: string; dashArray: string; dashOffset: number }[] = [];
  private maxProfit = 1;

  ngOnInit(): void {
    this.http.get<DashboardResponse>('/api/dashboard?months=6').subscribe(res => {
      this.data = res;
      this.maxProfit = Math.max(1, ...res.monthlyData.map(m => Math.abs(m.profit)));
    });

    this.http.get<{ content: CalculationRecord[] }>('/api/calculations?page=0&size=100').subscribe(res => {
      if (res.content.length > 0) {
        const fuel = res.content.reduce((s, c) => s + (c.fuelCost || 0), 0);
        const toll = res.content.reduce((s, c) => s + (c.tollCost || 0), 0);
        const driver = res.content.reduce((s, c) => s + (c.driverDailyCost || 0), 0);
        const other = res.content.reduce((s, c) => s + (c.otherCosts || 0), 0);
        const total = fuel + toll + driver + other;
        this.costBreakdown = { fuel, toll, driver, other, total };
        this.buildDonut(total);
      }
    });
  }

  getBarHeight(profit: number): number {
    return Math.max(8, (Math.abs(profit) / this.maxProfit) * 100);
  }

  formatMonth(month: string): string {
    const parts = month.split('-');
    const months = ['', 'Jaan', 'Veebr', 'Märts', 'Apr', 'Mai', 'Juuni', 'Juuli', 'Aug', 'Sept', 'Okt', 'Nov', 'Dets'];
    return months[parseInt(parts[1])] || month;
  }

  private buildDonut(total: number): void {
    if (!this.costBreakdown || total === 0) return;
    const circumference = 2 * Math.PI * 70;
    const items = [
      { label: 'calculator.fuel_cost', value: this.costBreakdown.fuel, color: '#1a237e' },
      { label: 'calculator.toll_cost', value: this.costBreakdown.toll, color: '#4caf50' },
      { label: 'calculator.driver_cost', value: this.costBreakdown.driver, color: '#ff9800' },
      { label: 'calculator.other_costs_label', value: this.costBreakdown.other, color: '#9c27b0' },
    ].filter(i => i.value > 0);

    let offset = 0;
    this.donutSegments = items.map(item => {
      const pct = (item.value / total) * 100;
      const length = (pct / 100) * circumference;
      const seg = {
        ...item,
        pct,
        dashArray: `${length} ${circumference - length}`,
        dashOffset: -offset
      };
      offset += length;
      return seg;
    });
  }
}
