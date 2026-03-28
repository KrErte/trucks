import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { TranslateModule } from '@ngx-translate/core';
import { AnalyticsService, LaneStats, CostTrend } from '../shared/services/analytics.service';

@Component({
  selector: 'app-analytics',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatCardModule, MatTableModule, MatSortModule,
    MatButtonModule, MatIconModule, MatFormFieldModule, MatInputModule, MatSelectModule,
    TranslateModule
  ],
  template: `
    <div class="analytics-container">
      <h1>{{ 'analytics.title' | translate }}</h1>

      <!-- Lane Statistics -->
      <mat-card class="section-card">
        <h2>{{ 'analytics.lane_stats' | translate }}</h2>
        <table mat-table [dataSource]="laneStats" matSort (matSortChange)="sortLanes($event)" class="full-width">
          <ng-container matColumnDef="lane">
            <th mat-header-cell *matHeaderCellDef>{{ 'analytics.lane' | translate }}</th>
            <td mat-cell *matCellDef="let l">{{ l.origin }} → {{ l.destination }}</td>
          </ng-container>
          <ng-container matColumnDef="tripCount">
            <th mat-header-cell *matHeaderCellDef mat-sort-header>{{ 'analytics.trip_count' | translate }}</th>
            <td mat-cell *matCellDef="let l">{{ l.tripCount }}</td>
          </ng-container>
          <ng-container matColumnDef="avgProfit">
            <th mat-header-cell *matHeaderCellDef mat-sort-header>{{ 'analytics.avg_profit' | translate }}</th>
            <td mat-cell *matCellDef="let l" [class.profit-positive]="l.avgProfit >= 0" [class.profit-negative]="l.avgProfit < 0">
              {{ l.avgProfit | number:'1.2-2' }} EUR
            </td>
          </ng-container>
          <ng-container matColumnDef="avgMargin">
            <th mat-header-cell *matHeaderCellDef mat-sort-header>{{ 'analytics.avg_margin' | translate }}</th>
            <td mat-cell *matCellDef="let l">{{ l.avgMargin | number:'1.1-1' }}%</td>
          </ng-container>
          <ng-container matColumnDef="totalRevenue">
            <th mat-header-cell *matHeaderCellDef mat-sort-header>{{ 'analytics.total_revenue' | translate }}</th>
            <td mat-cell *matCellDef="let l">{{ l.totalRevenue | number:'1.2-2' }} EUR</td>
          </ng-container>

          <tr mat-header-row *matHeaderRowDef="laneColumns"></tr>
          <tr mat-row *matRowDef="let row; columns: laneColumns;"></tr>
        </table>
      </mat-card>

      <!-- Cost Trends Chart -->
      <mat-card class="section-card">
        <h2>{{ 'analytics.cost_trends' | translate }}</h2>
        @if (costTrends.length > 0) {
          <div class="chart-container">
            <div class="chart-bars">
              @for (t of costTrends; track t.month) {
                <div class="chart-col">
                  <div class="chart-bar-group">
                    <div class="chart-bar revenue-bar" [style.height.px]="getBarHeight(t.avgRevenue, maxTrendValue)" title="Revenue: {{ t.avgRevenue | number:'1.0-0' }} EUR"></div>
                    <div class="chart-bar cost-bar" [style.height.px]="getBarHeight(t.avgTotalCost, maxTrendValue)" title="Cost: {{ t.avgTotalCost | number:'1.0-0' }} EUR"></div>
                    <div class="chart-bar profit-bar" [class.negative]="t.avgProfit < 0" [style.height.px]="getBarHeight(Math.abs(t.avgProfit), maxTrendValue)" title="Profit: {{ t.avgProfit | number:'1.0-0' }} EUR"></div>
                  </div>
                  <span class="chart-label">{{ t.month }}</span>
                  <span class="chart-sublabel">{{ t.tripCount }} {{ 'analytics.trip_count' | translate }}</span>
                </div>
              }
            </div>
            <div class="chart-legend">
              <span class="legend-item"><span class="legend-dot revenue-bg"></span> {{ 'analytics.revenue' | translate }}</span>
              <span class="legend-item"><span class="legend-dot cost-bg"></span> {{ 'analytics.costs' | translate }}</span>
              <span class="legend-item"><span class="legend-dot profit-bg"></span> {{ 'analytics.profit' | translate }}</span>
            </div>
          </div>
        }

        <table mat-table [dataSource]="costTrends" class="full-width">
          <ng-container matColumnDef="month">
            <th mat-header-cell *matHeaderCellDef>{{ 'analytics.period' | translate }}</th>
            <td mat-cell *matCellDef="let t">{{ t.month }}</td>
          </ng-container>
          <ng-container matColumnDef="tripCount">
            <th mat-header-cell *matHeaderCellDef>{{ 'analytics.trip_count' | translate }}</th>
            <td mat-cell *matCellDef="let t">{{ t.tripCount }}</td>
          </ng-container>
          <ng-container matColumnDef="avgRevenue">
            <th mat-header-cell *matHeaderCellDef>{{ 'analytics.revenue' | translate }}</th>
            <td mat-cell *matCellDef="let t">{{ t.avgRevenue | number:'1.2-2' }} EUR</td>
          </ng-container>
          <ng-container matColumnDef="avgTotalCost">
            <th mat-header-cell *matHeaderCellDef>{{ 'analytics.costs' | translate }}</th>
            <td mat-cell *matCellDef="let t">{{ t.avgTotalCost | number:'1.2-2' }} EUR</td>
          </ng-container>
          <ng-container matColumnDef="avgProfit">
            <th mat-header-cell *matHeaderCellDef>{{ 'analytics.profit' | translate }}</th>
            <td mat-cell *matCellDef="let t" [class.profit-positive]="t.avgProfit >= 0" [class.profit-negative]="t.avgProfit < 0">
              {{ t.avgProfit | number:'1.2-2' }} EUR
            </td>
          </ng-container>

          <tr mat-header-row *matHeaderRowDef="trendColumns"></tr>
          <tr mat-row *matRowDef="let row; columns: trendColumns;"></tr>
        </table>
      </mat-card>

      <!-- Monthly Report Export -->
      <mat-card class="section-card">
        <h2>{{ 'analytics.monthly_report' | translate }}</h2>
        <div class="export-row">
          <mat-form-field appearance="outline">
            <mat-label>{{ 'analytics.period' | translate }}</mat-label>
            <input matInput [(ngModel)]="reportMonth" placeholder="yyyy-MM" />
          </mat-form-field>
          <button mat-raised-button color="primary" (click)="exportReport()" [disabled]="!reportMonth">
            <mat-icon>download</mat-icon> {{ 'analytics.export_report' | translate }}
          </button>
        </div>
      </mat-card>
    </div>
  `,
  styles: [`
    .analytics-container { padding: 24px; max-width: 1200px; margin: 0 auto; }
    .section-card { margin-bottom: 24px; padding: 16px; }
    .full-width { width: 100%; }
    h2 { margin: 0 0 16px; font-weight: 400; }
    .export-row { display: flex; gap: 16px; align-items: center; flex-wrap: wrap; }
    .profit-positive { color: #2e7d32; }
    .profit-negative { color: #c62828; }
    .chart-container { margin-bottom: 24px; }
    .chart-bars { display: flex; align-items: flex-end; gap: 8px; height: 200px; padding: 0 8px; border-bottom: 1px solid #e0e0e0; }
    .chart-col { flex: 1; display: flex; flex-direction: column; align-items: center; }
    .chart-bar-group { display: flex; align-items: flex-end; gap: 3px; }
    .chart-bar { width: 18px; border-radius: 3px 3px 0 0; min-height: 2px; transition: height 0.3s ease; }
    .revenue-bar { background: #42a5f5; }
    .cost-bar { background: #ef5350; }
    .profit-bar { background: #66bb6a; }
    .profit-bar.negative { background: #ef5350; opacity: 0.6; }
    .chart-label { font-size: 11px; color: #666; margin-top: 6px; }
    .chart-sublabel { font-size: 10px; color: #999; }
    .chart-legend { display: flex; justify-content: center; gap: 24px; margin-top: 12px; }
    .legend-item { display: flex; align-items: center; gap: 6px; font-size: 13px; color: #555; }
    .legend-dot { width: 12px; height: 12px; border-radius: 3px; }
    .revenue-bg { background: #42a5f5; }
    .cost-bg { background: #ef5350; }
    .profit-bg { background: #66bb6a; }
    @media (max-width: 768px) {
      .analytics-container { padding: 12px; }
      table { display: block; overflow-x: auto; }
      .chart-bar { width: 12px; }
    }
  `]
})
export class AnalyticsComponent implements OnInit {
  private analyticsService = inject(AnalyticsService);

  Math = Math;
  laneStats: LaneStats[] = [];
  costTrends: CostTrend[] = [];
  maxTrendValue = 1;
  laneColumns = ['lane', 'tripCount', 'avgProfit', 'avgMargin', 'totalRevenue'];
  trendColumns = ['month', 'tripCount', 'avgRevenue', 'avgTotalCost', 'avgProfit'];
  reportMonth = '';

  ngOnInit(): void {
    this.analyticsService.getLaneStats().subscribe(s => this.laneStats = s);
    this.analyticsService.getCostTrends(12).subscribe(t => {
      this.costTrends = t;
      this.maxTrendValue = Math.max(1, ...t.map(x => Math.max(x.avgRevenue, x.avgTotalCost, Math.abs(x.avgProfit))));
    });

    const now = new Date();
    this.reportMonth = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
  }

  sortLanes(sort: Sort): void {
    if (!sort.active || sort.direction === '') return;
    this.laneStats = [...this.laneStats].sort((a, b) => {
      const isAsc = sort.direction === 'asc';
      const key = sort.active as keyof LaneStats;
      const va = (a as any)[key];
      const vb = (b as any)[key];
      return (typeof va === 'number' ? va - vb : String(va).localeCompare(String(vb))) * (isAsc ? 1 : -1);
    });
  }

  getBarHeight(value: number, max: number): number {
    return Math.max(2, (value / max) * 160);
  }

  exportReport(): void {
    this.analyticsService.exportReport(this.reportMonth).subscribe(blob => {
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `report-${this.reportMonth}.xlsx`;
      a.click();
      window.URL.revokeObjectURL(url);
    });
  }
}
