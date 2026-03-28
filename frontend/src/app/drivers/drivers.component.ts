import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatChipsModule } from '@angular/material/chips';
import { TranslateModule } from '@ngx-translate/core';
import { DriverService, Driver } from '../shared/services/driver.service';

@Component({
  selector: 'app-drivers',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatCardModule, MatTableModule, MatButtonModule,
    MatIconModule, MatFormFieldModule, MatInputModule, MatSelectModule,
    MatExpansionModule, MatChipsModule, TranslateModule
  ],
  template: `
    <div class="page-container">
      <div class="page-header">
        <h1>{{ 'drivers.title' | translate }}</h1>
        <button mat-raised-button color="primary" (click)="showForm = !showForm; resetForm()">
          <mat-icon>{{ showForm ? 'close' : 'person_add' }}</mat-icon>
          {{ (showForm ? 'vehicles.cancel' : 'drivers.add') | translate }}
        </button>
      </div>

      @if (showForm) {
        <mat-card class="form-card">
          <div class="form-grid">
            <mat-form-field appearance="outline">
              <mat-label>{{ 'drivers.first_name' | translate }}</mat-label>
              <input matInput [(ngModel)]="form.firstName" required />
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>{{ 'drivers.last_name' | translate }}</mat-label>
              <input matInput [(ngModel)]="form.lastName" required />
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>{{ 'drivers.email' | translate }}</mat-label>
              <input matInput [(ngModel)]="form.email" type="email" />
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>{{ 'drivers.phone' | translate }}</mat-label>
              <input matInput [(ngModel)]="form.phone" />
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>{{ 'drivers.daily_rate' | translate }}</mat-label>
              <input matInput [(ngModel)]="form.dailyRate" type="number" />
            </mat-form-field>
          </div>

          <mat-expansion-panel class="details-panel">
            <mat-expansion-panel-header>
              <mat-panel-title>{{ 'drivers.license_docs' | translate }}</mat-panel-title>
            </mat-expansion-panel-header>
            <div class="form-grid">
              <mat-form-field appearance="outline">
                <mat-label>{{ 'drivers.license_number' | translate }}</mat-label>
                <input matInput [(ngModel)]="form.licenseNumber" />
              </mat-form-field>
              <mat-form-field appearance="outline">
                <mat-label>{{ 'drivers.license_expiry' | translate }}</mat-label>
                <input matInput [(ngModel)]="form.licenseExpiry" type="date" />
              </mat-form-field>
              <mat-form-field appearance="outline">
                <mat-label>{{ 'drivers.license_categories' | translate }}</mat-label>
                <input matInput [(ngModel)]="form.licenseCategories" placeholder="C, CE, ..." />
              </mat-form-field>
              <mat-form-field appearance="outline">
                <mat-label>{{ 'drivers.driver_card' | translate }}</mat-label>
                <input matInput [(ngModel)]="form.driverCardNumber" />
              </mat-form-field>
              <mat-form-field appearance="outline">
                <mat-label>{{ 'drivers.driver_card_expiry' | translate }}</mat-label>
                <input matInput [(ngModel)]="form.driverCardExpiry" type="date" />
              </mat-form-field>
              <mat-form-field appearance="outline">
                <mat-label>{{ 'drivers.adr_expiry' | translate }}</mat-label>
                <input matInput [(ngModel)]="form.adrCertificateExpiry" type="date" />
              </mat-form-field>
            </div>
          </mat-expansion-panel>

          <mat-form-field appearance="outline" class="full-width notes-field">
            <mat-label>{{ 'drivers.notes' | translate }}</mat-label>
            <textarea matInput [(ngModel)]="form.notes" rows="2"></textarea>
          </mat-form-field>

          <button mat-raised-button color="primary" (click)="save()" [disabled]="!form.firstName || !form.lastName">
            <mat-icon>save</mat-icon> {{ (editingId ? 'vehicles.update' : 'vehicles.add_btn') | translate }}
          </button>
        </mat-card>
      }

      <table mat-table [dataSource]="drivers" class="full-width">
        <ng-container matColumnDef="name">
          <th mat-header-cell *matHeaderCellDef>{{ 'drivers.name' | translate }}</th>
          <td mat-cell *matCellDef="let d">{{ d.firstName }} {{ d.lastName }}</td>
        </ng-container>
        <ng-container matColumnDef="phone">
          <th mat-header-cell *matHeaderCellDef>{{ 'drivers.phone' | translate }}</th>
          <td mat-cell *matCellDef="let d">{{ d.phone }}</td>
        </ng-container>
        <ng-container matColumnDef="licenseExpiry">
          <th mat-header-cell *matHeaderCellDef>{{ 'drivers.license_expiry' | translate }}</th>
          <td mat-cell *matCellDef="let d" [class.expiring-soon]="isExpiringSoon(d.licenseExpiry)" [class.expired]="isExpired(d.licenseExpiry)">
            {{ d.licenseExpiry || '-' }}
            @if (isExpired(d.licenseExpiry)) { <mat-icon class="warn-icon">error</mat-icon> }
            @else if (isExpiringSoon(d.licenseExpiry)) { <mat-icon class="warn-icon">warning</mat-icon> }
          </td>
        </ng-container>
        <ng-container matColumnDef="driverCardExpiry">
          <th mat-header-cell *matHeaderCellDef>{{ 'drivers.driver_card_expiry' | translate }}</th>
          <td mat-cell *matCellDef="let d" [class.expiring-soon]="isExpiringSoon(d.driverCardExpiry)" [class.expired]="isExpired(d.driverCardExpiry)">
            {{ d.driverCardExpiry || '-' }}
            @if (isExpired(d.driverCardExpiry)) { <mat-icon class="warn-icon">error</mat-icon> }
            @else if (isExpiringSoon(d.driverCardExpiry)) { <mat-icon class="warn-icon">warning</mat-icon> }
          </td>
        </ng-container>
        <ng-container matColumnDef="dailyRate">
          <th mat-header-cell *matHeaderCellDef>{{ 'drivers.daily_rate' | translate }}</th>
          <td mat-cell *matCellDef="let d">{{ d.dailyRate | number:'1.2-2' }} EUR</td>
        </ng-container>
        <ng-container matColumnDef="status">
          <th mat-header-cell *matHeaderCellDef>{{ 'users.status' | translate }}</th>
          <td mat-cell *matCellDef="let d">
            <span class="status-badge" [class.active]="d.active" [class.inactive]="!d.active">
              {{ (d.active ? 'users.active' : 'users.inactive') | translate }}
            </span>
          </td>
        </ng-container>
        <ng-container matColumnDef="actions">
          <th mat-header-cell *matHeaderCellDef>{{ 'vehicles.actions' | translate }}</th>
          <td mat-cell *matCellDef="let d">
            <button mat-icon-button (click)="edit(d)"><mat-icon>edit</mat-icon></button>
            @if (d.active) {
              <button mat-icon-button (click)="toggleActive(d)"><mat-icon>block</mat-icon></button>
            } @else {
              <button mat-icon-button (click)="toggleActive(d)"><mat-icon>check_circle</mat-icon></button>
            }
            <button mat-icon-button color="warn" (click)="deleteDriver(d.id)"><mat-icon>delete</mat-icon></button>
          </td>
        </ng-container>

        <tr mat-header-row *matHeaderRowDef="columns"></tr>
        <tr mat-row *matRowDef="let row; columns: columns;"></tr>
      </table>
    </div>
  `,
  styles: [`
    .page-container { padding: 24px; max-width: 1200px; margin: 0 auto; }
    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
    .page-header h1 { margin: 0; }
    .form-card { padding: 24px; margin-bottom: 24px; }
    .form-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 12px; margin-bottom: 16px; }
    .full-width { width: 100%; }
    .notes-field { margin: 12px 0; }
    .details-panel { margin-bottom: 16px; }
    .status-badge { padding: 4px 12px; border-radius: 12px; font-size: 12px; font-weight: 500; }
    .status-badge.active { background: #e8f5e9; color: #2e7d32; }
    .status-badge.inactive { background: #fce4ec; color: #c62828; }
    .expiring-soon { color: #e65100; }
    .expired { color: #c62828; }
    .warn-icon { font-size: 16px; width: 16px; height: 16px; vertical-align: middle; margin-left: 4px; }
    @media (max-width: 768px) {
      .page-container { padding: 12px; }
      table { display: block; overflow-x: auto; }
    }
  `]
})
export class DriversComponent implements OnInit {
  private driverService = inject(DriverService);

  drivers: Driver[] = [];
  showForm = false;
  editingId: string | null = null;
  form: Driver = this.emptyForm();
  columns = ['name', 'phone', 'licenseExpiry', 'driverCardExpiry', 'dailyRate', 'status', 'actions'];

  ngOnInit(): void { this.load(); }

  load(): void {
    this.driverService.getAll().subscribe(d => this.drivers = d);
  }

  emptyForm(): Driver {
    return { firstName: '', lastName: '', email: '', phone: '', licenseNumber: '', licenseExpiry: '', licenseCategories: '', driverCardNumber: '', driverCardExpiry: '', adrCertificateExpiry: '', dailyRate: 0, notes: '' };
  }

  resetForm(): void { this.form = this.emptyForm(); this.editingId = null; }

  save(): void {
    if (this.editingId) {
      this.driverService.update(this.editingId, this.form).subscribe(() => { this.load(); this.showForm = false; this.resetForm(); });
    } else {
      this.driverService.create(this.form).subscribe(() => { this.load(); this.showForm = false; this.resetForm(); });
    }
  }

  edit(d: Driver): void {
    this.form = { ...d };
    this.editingId = d.id!;
    this.showForm = true;
  }

  toggleActive(d: Driver): void {
    const obs = d.active ? this.driverService.deactivate(d.id!) : this.driverService.activate(d.id!);
    obs.subscribe(() => this.load());
  }

  deleteDriver(id: string): void {
    this.driverService.delete(id).subscribe(() => this.load());
  }

  isExpiringSoon(date?: string): boolean {
    if (!date) return false;
    const d = new Date(date);
    const now = new Date();
    const diff = (d.getTime() - now.getTime()) / (1000 * 60 * 60 * 24);
    return diff > 0 && diff <= 30;
  }

  isExpired(date?: string): boolean {
    if (!date) return false;
    return new Date(date) < new Date();
  }
}
