import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatChipsModule } from '@angular/material/chips';
import { TranslateModule } from '@ngx-translate/core';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { CalculationService } from '../shared/services/calculation.service';
import { InvoiceService } from '../shared/services/invoice.service';
import { CalculationResponse } from '../shared/models/calculation.model';

@Component({
  selector: 'app-history',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatCardModule, MatTableModule, MatPaginatorModule,
    MatIconModule, MatButtonModule, MatFormFieldModule, MatInputModule,
    MatSelectModule, MatSortModule, MatChipsModule, MatDialogModule, TranslateModule
  ],
  template: `
    <div class="history-container">
      <div class="history-header">
        <h1>{{ 'history.title' | translate }}</h1>
        <button mat-raised-button color="primary" (click)="exportAll()">
          <mat-icon>download</mat-icon> {{ 'history.export_all' | translate }}
        </button>
      </div>

      @if (monthlySummary.length > 0) {
        <mat-card class="summary-card">
          <h2>{{ 'history.monthly_summary' | translate }}</h2>
          <table class="summary-table">
            <thead>
              <tr>
                <th>{{ 'history.month_col' | translate }}</th>
                <th>{{ 'history.trip_count' | translate }}</th>
                <th>{{ 'history.total_revenue' | translate }}</th>
                <th>{{ 'history.total_cost' | translate }}</th>
                <th>{{ 'history.profit' | translate }}</th>
                <th>{{ 'history.avg_margin' | translate }}</th>
              </tr>
            </thead>
            <tbody>
              @for (m of monthlySummary; track m.month) {
                <tr>
                  <td>{{ m.month }}</td>
                  <td>{{ m.tripCount }}</td>
                  <td>{{ m.totalRevenue | number:'1.2-2' }} €</td>
                  <td>{{ m.totalCost | number:'1.2-2' }} €</td>
                  <td [class.profit-positive]="m.totalProfit >= 0" [class.profit-negative]="m.totalProfit < 0">
                    {{ m.totalProfit | number:'1.2-2' }} €
                  </td>
                  <td>{{ m.avgMargin | number:'1.1-1' }}%</td>
                </tr>
              }
            </tbody>
          </table>
        </mat-card>
      }

      @if (belowThresholdCount > 0) {
        <div class="alert-banner">
          <mat-icon>warning</mat-icon>
          <span>{{ 'history.margin_alert' | translate:{ count: belowThresholdCount, threshold: minMarginThreshold } }}</span>
        </div>
      }

      <mat-card class="filter-card">
        <div class="filters">
          <mat-form-field appearance="outline" class="filter-field">
            <mat-label>{{ 'history.search_route' | translate }}</mat-label>
            <mat-icon matPrefix>search</mat-icon>
            <input matInput [(ngModel)]="searchText" (input)="applyFilters()" placeholder="{{ 'history.search_placeholder' | translate }}" />
          </mat-form-field>

          <mat-form-field appearance="outline" class="filter-field-small">
            <mat-label>{{ 'history.min_margin' | translate }}</mat-label>
            <input matInput type="number" [(ngModel)]="minMargin" (input)="applyFilters()" />
            <span matSuffix>%</span>
          </mat-form-field>

          <mat-form-field appearance="outline" class="filter-field-small">
            <mat-label>{{ 'history.profit_filter' | translate }}</mat-label>
            <mat-select [(ngModel)]="profitFilter" (selectionChange)="applyFilters()">
              <mat-option value="all">{{ 'history.all' | translate }}</mat-option>
              <mat-option value="profitable">{{ 'history.profitable' | translate }}</mat-option>
              <mat-option value="unprofitable">{{ 'history.unprofitable' | translate }}</mat-option>
            </mat-select>
          </mat-form-field>

          @if (searchText || minMargin !== null || profitFilter !== 'all') {
            <button mat-icon-button (click)="clearFilters()" title="{{ 'history.clear_filters' | translate }}">
              <mat-icon>clear</mat-icon>
            </button>
          }
        </div>

        @if (filteredCalculations.length !== allCalculations.length) {
          <div class="filter-info">
            {{ filteredCalculations.length }} / {{ allCalculations.length }} {{ 'history.results' | translate }}
          </div>
        }
      </mat-card>

      <mat-card>
        <table mat-table [dataSource]="filteredCalculations" matSort (matSortChange)="sortData($event)" class="full-width">
          <ng-container matColumnDef="createdAt">
            <th mat-header-cell *matHeaderCellDef mat-sort-header>{{ 'history.date' | translate }}</th>
            <td mat-cell *matCellDef="let c">{{ c.createdAt | date:'dd.MM.yyyy HH:mm' }}</td>
          </ng-container>
          <ng-container matColumnDef="route">
            <th mat-header-cell *matHeaderCellDef>{{ 'history.route' | translate }}</th>
            <td mat-cell *matCellDef="let c">{{ c.origin }} → {{ c.destination }}</td>
          </ng-container>
          <ng-container matColumnDef="vehicleName">
            <th mat-header-cell *matHeaderCellDef mat-sort-header>{{ 'history.vehicle' | translate }}</th>
            <td mat-cell *matCellDef="let c">{{ c.vehicleName }}</td>
          </ng-container>
          <ng-container matColumnDef="orderPrice">
            <th mat-header-cell *matHeaderCellDef mat-sort-header>{{ 'history.order_price' | translate }}</th>
            <td mat-cell *matCellDef="let c">{{ c.orderPrice | number:'1.2-2' }} EUR</td>
          </ng-container>
          <ng-container matColumnDef="totalCost">
            <th mat-header-cell *matHeaderCellDef mat-sort-header>{{ 'history.total_cost' | translate }}</th>
            <td mat-cell *matCellDef="let c">{{ c.totalCost | number:'1.2-2' }} EUR</td>
          </ng-container>
          <ng-container matColumnDef="profit">
            <th mat-header-cell *matHeaderCellDef mat-sort-header>{{ 'history.profit' | translate }}</th>
            <td mat-cell *matCellDef="let c" [class.profit-positive]="c.profit >= 0" [class.profit-negative]="c.profit < 0">
              {{ c.profit | number:'1.2-2' }} EUR
            </td>
          </ng-container>
          <ng-container matColumnDef="profitMarginPct">
            <th mat-header-cell *matHeaderCellDef mat-sort-header>{{ 'history.margin' | translate }}</th>
            <td mat-cell *matCellDef="let c">
              <div class="margin-cell">
                <div class="margin-bar-track">
                  <div class="margin-bar-fill"
                    [class.high]="c.profitMarginPct >= 20"
                    [class.medium]="c.profitMarginPct >= 10 && c.profitMarginPct < 20"
                    [class.low]="c.profitMarginPct >= 0 && c.profitMarginPct < 10"
                    [class.negative]="c.profitMarginPct < 0"
                    [style.width.%]="getMarginWidth(c.profitMarginPct)">
                  </div>
                </div>
                <span [class.profit-positive]="c.profitMarginPct >= 0" [class.profit-negative]="c.profitMarginPct < 0">
                  {{ c.profitMarginPct | number:'1.1-1' }}%
                </span>
              </div>
            </td>
          </ng-container>
          <ng-container matColumnDef="actions">
            <th mat-header-cell *matHeaderCellDef></th>
            <td mat-cell *matCellDef="let c">
              <button mat-icon-button (click)="downloadPdf(c.id)" title="PDF">
                <mat-icon>picture_as_pdf</mat-icon>
              </button>
              <button mat-icon-button (click)="downloadExcel(c.id)" title="Excel">
                <mat-icon>table_chart</mat-icon>
              </button>
              <button mat-icon-button (click)="createInvoice(c)" title="{{ 'invoices.create' | translate }}">
                <mat-icon>receipt</mat-icon>
              </button>
            </td>
          </ng-container>

          <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
          <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
        </table>

        <mat-paginator
          [length]="totalElements"
          [pageSize]="pageSize"
          [pageSizeOptions]="[10, 20, 50]"
          (page)="onPage($event)"
          showFirstLastButtons>
        </mat-paginator>
      </mat-card>
    </div>
  `,
  styleUrl: './history.component.scss'
})
export class HistoryComponent implements OnInit {
  private calculationService = inject(CalculationService);
  private invoiceService = inject(InvoiceService);
  private http = inject(HttpClient);

  allCalculations: CalculationResponse[] = [];
  filteredCalculations: CalculationResponse[] = [];
  displayedColumns = ['createdAt', 'route', 'vehicleName', 'orderPrice', 'totalCost', 'profit', 'profitMarginPct', 'actions'];
  totalElements = 0;
  pageSize = 20;

  searchText = '';
  minMargin: number | null = null;
  profitFilter = 'all';
  minMarginThreshold = 10;
  belowThresholdCount = 0;
  monthlySummary: { month: string; tripCount: number; totalRevenue: number; totalCost: number; totalProfit: number; avgMargin: number }[] = [];

  ngOnInit(): void {
    this.loadHistory(0);
    this.loadMonthlySummary();
  }

  loadMonthlySummary(): void {
    this.http.get<any[]>('/api/calculations/summary?months=12').subscribe(res => {
      this.monthlySummary = res;
    });
  }

  loadHistory(page: number): void {
    const savedThreshold = localStorage.getItem('minMarginPct');
    if (savedThreshold) this.minMarginThreshold = parseFloat(savedThreshold);

    this.calculationService.getHistory(page, this.pageSize).subscribe(res => {
      this.allCalculations = res.content;
      this.totalElements = res.totalElements;
      this.belowThresholdCount = this.allCalculations.filter(c => c.profitMarginPct < this.minMarginThreshold).length;
      this.applyFilters();
    });
  }

  applyFilters(): void {
    let result = [...this.allCalculations];

    if (this.searchText.trim()) {
      const q = this.searchText.toLowerCase();
      result = result.filter(c =>
        (c.origin + ' ' + c.destination + ' ' + c.vehicleName).toLowerCase().includes(q)
      );
    }

    if (this.minMargin !== null && this.minMargin !== undefined) {
      result = result.filter(c => c.profitMarginPct >= this.minMargin!);
    }

    if (this.profitFilter === 'profitable') {
      result = result.filter(c => c.profit >= 0);
    } else if (this.profitFilter === 'unprofitable') {
      result = result.filter(c => c.profit < 0);
    }

    this.filteredCalculations = result;
  }

  clearFilters(): void {
    this.searchText = '';
    this.minMargin = null;
    this.profitFilter = 'all';
    this.applyFilters();
  }

  sortData(sort: Sort): void {
    if (!sort.active || sort.direction === '') {
      this.applyFilters();
      return;
    }
    this.filteredCalculations = [...this.filteredCalculations].sort((a, b) => {
      const isAsc = sort.direction === 'asc';
      const key = sort.active as keyof CalculationResponse;
      const va = (a as any)[key];
      const vb = (b as any)[key];
      if (typeof va === 'number' && typeof vb === 'number') {
        return (va - vb) * (isAsc ? 1 : -1);
      }
      return String(va).localeCompare(String(vb)) * (isAsc ? 1 : -1);
    });
  }

  getMarginWidth(margin: number): number {
    return Math.min(100, Math.max(0, margin));
  }

  onPage(event: PageEvent): void {
    this.pageSize = event.pageSize;
    this.loadHistory(event.pageIndex);
  }

  downloadExcel(id: string): void {
    this.http.get(`/api/calculations/${id}/excel`, { responseType: 'blob' }).subscribe(blob => {
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `calculation-${id}.xlsx`;
      a.click();
      window.URL.revokeObjectURL(url);
    });
  }

  exportAll(): void {
    this.http.get('/api/calculations/export/excel', { responseType: 'blob' }).subscribe(blob => {
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'calculations-export.xlsx';
      a.click();
      window.URL.revokeObjectURL(url);
    });
  }

  createInvoice(calc: CalculationResponse): void {
    const customerName = prompt('Customer name / Kliendi nimi:');
    if (!customerName) return;
    const customerVat = prompt('Customer VAT / Käibemaksu nr (optional):') || '';
    this.invoiceService.createInvoice(calc.id, customerName, customerVat, 14).subscribe({
      next: () => alert('Invoice created / Arve loodud!'),
      error: (err: any) => alert('Error: ' + (err.error?.message || err.message))
    });
  }

  downloadPdf(id: string): void {
    this.http.get(`/api/calculations/${id}/pdf`, { responseType: 'blob' }).subscribe(blob => {
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `calculation-${id}.pdf`;
      a.click();
      window.URL.revokeObjectURL(url);
    });
  }
}
