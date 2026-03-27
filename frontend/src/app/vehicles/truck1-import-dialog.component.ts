import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder } from '@angular/forms';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { TranslateModule } from '@ngx-translate/core';
import { ScraperService } from '../shared/services/scraper.service';
import { ScrapedTruckData } from '../shared/models/vehicle.model';

@Component({
  selector: 'app-truck1-import-dialog',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatDialogModule, MatButtonModule,
    MatIconModule, MatFormFieldModule, MatInputModule, MatSelectModule,
    MatTableModule, MatProgressSpinnerModule, MatChipsModule, TranslateModule
  ],
  template: `
    <h2 mat-dialog-title>
      <mat-icon style="vertical-align: middle; margin-right: 8px">cloud_download</mat-icon>
      {{ 'vehicles.import_from_truck1' | translate }}
    </h2>
    <mat-dialog-content>
      <!-- Search form -->
      <form [formGroup]="searchForm" (ngSubmit)="search()" class="search-form">
        <mat-form-field appearance="outline">
          <mat-label>{{ 'vehicles.brand' | translate }}</mat-label>
          <mat-select formControlName="brand">
            <mat-option value="scania">Scania</mat-option>
            <mat-option value="volvo">Volvo</mat-option>
            <mat-option value="man">MAN</mat-option>
            <mat-option value="mercedes-benz">Mercedes-Benz</mat-option>
            <mat-option value="daf">DAF</mat-option>
            <mat-option value="iveco">Iveco</mat-option>
            <mat-option value="renault">Renault</mat-option>
          </mat-select>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>{{ 'vehicles.type' | translate }}</mat-label>
          <mat-select formControlName="type">
            <mat-option value="tractor-units">{{ 'vehicles.tractor_units' | translate }}</mat-option>
            <mat-option value="rigid-trucks">{{ 'vehicles.rigid_trucks' | translate }}</mat-option>
            <mat-option value="tippers">{{ 'vehicles.tippers' | translate }}</mat-option>
          </mat-select>
        </mat-form-field>
        <button mat-raised-button color="primary" type="submit" [disabled]="loading">
          <mat-icon>search</mat-icon> {{ 'vehicles.search' | translate }}
        </button>
      </form>

      @if (loading) {
        <div class="spinner-container">
          <mat-spinner diameter="40"></mat-spinner>
        </div>
      }

      @if (errorMessage) {
        <p class="error-text">{{ errorMessage }}</p>
      }

      <!-- Search results -->
      @if (results.length > 0 && !selectedTruck) {
        <table mat-table [dataSource]="results" class="full-width results-table">
          <ng-container matColumnDef="name">
            <th mat-header-cell *matHeaderCellDef>{{ 'vehicles.name' | translate }}</th>
            <td mat-cell *matCellDef="let t">{{ t.name }}</td>
          </ng-container>
          <ng-container matColumnDef="price">
            <th mat-header-cell *matHeaderCellDef>{{ 'vehicles.price' | translate }}</th>
            <td mat-cell *matCellDef="let t">{{ t.price ? (t.price | number:'1.0-0') + ' ' + (t.currency || 'EUR') : '-' }}</td>
          </ng-container>
          <ng-container matColumnDef="location">
            <th mat-header-cell *matHeaderCellDef>{{ 'vehicles.location' | translate }}</th>
            <td mat-cell *matCellDef="let t">{{ t.location || '-' }}</td>
          </ng-container>
          <ng-container matColumnDef="actions">
            <th mat-header-cell *matHeaderCellDef></th>
            <td mat-cell *matCellDef="let t">
              <button mat-icon-button color="primary" (click)="selectTruck(t)">
                <mat-icon>visibility</mat-icon>
              </button>
            </td>
          </ng-container>

          <tr mat-header-row *matHeaderRowDef="resultColumns"></tr>
          <tr mat-row *matRowDef="let row; columns: resultColumns;" class="clickable-row"></tr>
        </table>
      }

      @if (results.length === 0 && searched && !loading) {
        <p style="text-align: center; color: #666; padding: 24px 0;">
          {{ 'vehicles.no_results' | translate }}
        </p>
      }

      <!-- Detail view after selecting a truck -->
      @if (selectedTruck) {
        <div class="detail-view">
          <button mat-button (click)="selectedTruck = null">
            <mat-icon>arrow_back</mat-icon> {{ 'vehicles.back_to_results' | translate }}
          </button>
          <h3>{{ selectedTruck.name }}</h3>
          @if (detailLoading) {
            <mat-spinner diameter="30"></mat-spinner>
          } @else {
            <div class="detail-grid">
              @if (selectedTruck.brand) {
                <div class="detail-item"><strong>{{ 'vehicles.brand' | translate }}:</strong> {{ selectedTruck.brand }}</div>
              }
              @if (selectedTruck.year) {
                <div class="detail-item"><strong>{{ 'vehicles.year' | translate }}:</strong> {{ selectedTruck.year }}</div>
              }
              @if (selectedTruck.powerHp) {
                <div class="detail-item"><strong>{{ 'vehicles.power_hp' | translate }}:</strong> {{ selectedTruck.powerHp }} HP</div>
              }
              @if (selectedTruck.axleConfiguration) {
                <div class="detail-item"><strong>{{ 'vehicles.axle_config' | translate }}:</strong> {{ selectedTruck.axleConfiguration }}</div>
              }
              @if (selectedTruck.grossWeight) {
                <div class="detail-item"><strong>{{ 'vehicles.gross_weight' | translate }}:</strong> {{ selectedTruck.grossWeight }} t</div>
              }
              @if (selectedTruck.gearbox) {
                <div class="detail-item"><strong>{{ 'vehicles.gearbox' | translate }}:</strong> {{ selectedTruck.gearbox }}</div>
              }
              @if (selectedTruck.suspension) {
                <div class="detail-item"><strong>{{ 'vehicles.suspension' | translate }}:</strong> {{ selectedTruck.suspension }}</div>
              }
              @if (selectedTruck.euroClass) {
                <div class="detail-item"><strong>{{ 'vehicles.euro_class' | translate }}:</strong> {{ selectedTruck.euroClass }}</div>
              }
              @if (selectedTruck.tankCapacity) {
                <div class="detail-item"><strong>{{ 'vehicles.tank_capacity' | translate }}:</strong> {{ selectedTruck.tankCapacity }} L</div>
              }
              @if (selectedTruck.mileageKm) {
                <div class="detail-item"><strong>{{ 'vehicles.mileage' | translate }}:</strong> {{ selectedTruck.mileageKm | number }} km</div>
              }
              @if (selectedTruck.price) {
                <div class="detail-item"><strong>{{ 'vehicles.price' | translate }}:</strong> {{ selectedTruck.price | number:'1.0-0' }} {{ selectedTruck.currency || 'EUR' }}</div>
              }
            </div>
          }
        </div>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>{{ 'vehicles.cancel' | translate }}</button>
      @if (selectedTruck && !detailLoading) {
        <button mat-raised-button color="primary" (click)="importSelected()">
          <mat-icon>download</mat-icon> {{ 'vehicles.import_btn' | translate }}
        </button>
      }
    </mat-dialog-actions>
  `,
  styles: [`
    .search-form {
      display: flex;
      gap: 12px;
      align-items: flex-start;
      flex-wrap: wrap;
      margin-bottom: 16px;
    }
    .search-form mat-form-field { flex: 1; min-width: 150px; }
    .search-form button { margin-top: 4px; }
    .spinner-container { display: flex; justify-content: center; padding: 24px; }
    .full-width { width: 100%; }
    .results-table { margin-top: 8px; }
    .clickable-row { cursor: pointer; }
    .clickable-row:hover { background: rgba(0,0,0,0.04); }
    .detail-view { padding: 8px 0; }
    .detail-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 8px 24px;
      margin-top: 12px;
    }
    .detail-item { padding: 4px 0; }
    .error-text { color: #f44336; text-align: center; }
    mat-dialog-content { min-width: 500px; max-height: 70vh; }
    @media (max-width: 600px) {
      mat-dialog-content { min-width: unset; }
      .detail-grid { grid-template-columns: 1fr; }
    }
  `]
})
export class Truck1ImportDialogComponent {
  private fb = inject(FormBuilder);
  private scraperService = inject(ScraperService);
  private dialogRef = inject(MatDialogRef<Truck1ImportDialogComponent>);

  searchForm = this.fb.group({
    brand: ['scania'],
    type: ['tractor-units']
  });

  results: ScrapedTruckData[] = [];
  selectedTruck: ScrapedTruckData | null = null;
  loading = false;
  detailLoading = false;
  searched = false;
  errorMessage = '';
  resultColumns = ['name', 'price', 'location', 'actions'];

  search(): void {
    this.loading = true;
    this.searched = true;
    this.errorMessage = '';
    this.selectedTruck = null;
    const { brand, type } = this.searchForm.value;
    this.scraperService.searchTrucks(brand!, type!).subscribe({
      next: (data) => {
        this.results = data;
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = 'Search failed. Please try again.';
        this.loading = false;
      }
    });
  }

  selectTruck(truck: ScrapedTruckData): void {
    this.selectedTruck = truck;
    if (truck.sourceUrl) {
      this.detailLoading = true;
      this.scraperService.getTruckDetails(truck.sourceUrl).subscribe({
        next: (detailed) => {
          // Merge: keep search data, overlay detail data
          this.selectedTruck = { ...truck, ...detailed, name: detailed.name || truck.name };
          this.detailLoading = false;
        },
        error: () => {
          this.detailLoading = false;
        }
      });
    }
  }

  importSelected(): void {
    if (!this.selectedTruck) return;
    this.dialogRef.close(this.selectedTruck);
  }
}
