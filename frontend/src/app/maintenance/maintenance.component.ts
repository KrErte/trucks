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
import { TranslateModule } from '@ngx-translate/core';
import { MaintenanceService, MaintenanceRecord } from '../shared/services/maintenance.service';
import { HttpClient } from '@angular/common/http';

interface VehicleOption { id: string; name: string; }

@Component({
  selector: 'app-maintenance',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatCardModule, MatTableModule, MatButtonModule,
    MatIconModule, MatFormFieldModule, MatInputModule, MatSelectModule, TranslateModule
  ],
  template: `
    <div class="page-container">
      <div class="page-header">
        <h1>{{ 'maintenance.title' | translate }}</h1>
        <button mat-raised-button color="primary" (click)="showForm = !showForm; resetForm()">
          <mat-icon>{{ showForm ? 'close' : 'add' }}</mat-icon>
          {{ (showForm ? 'vehicles.cancel' : 'maintenance.add') | translate }}
        </button>
      </div>

      @if (showForm) {
        <mat-card class="form-card">
          <div class="form-grid">
            <mat-form-field appearance="outline">
              <mat-label>{{ 'maintenance.vehicle' | translate }}</mat-label>
              <mat-select [(ngModel)]="form.vehicleId" required>
                @for (v of vehicles; track v.id) {
                  <mat-option [value]="v.id">{{ v.name }}</mat-option>
                }
              </mat-select>
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>{{ 'maintenance.type' | translate }}</mat-label>
              <mat-select [(ngModel)]="form.type" required>
                <mat-option value="OIL_CHANGE">{{ 'maintenance.type_oil' | translate }}</mat-option>
                <mat-option value="TIRE_CHANGE">{{ 'maintenance.type_tire' | translate }}</mat-option>
                <mat-option value="BRAKE_SERVICE">{{ 'maintenance.type_brake' | translate }}</mat-option>
                <mat-option value="INSPECTION">{{ 'maintenance.type_inspection' | translate }}</mat-option>
                <mat-option value="INSURANCE">{{ 'maintenance.type_insurance' | translate }}</mat-option>
                <mat-option value="REPAIR">{{ 'maintenance.type_repair' | translate }}</mat-option>
                <mat-option value="OTHER">{{ 'maintenance.type_other' | translate }}</mat-option>
              </mat-select>
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>{{ 'maintenance.date' | translate }}</mat-label>
              <input matInput [(ngModel)]="form.performedAt" type="date" required />
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>{{ 'maintenance.cost' | translate }}</mat-label>
              <input matInput [(ngModel)]="form.cost" type="number" />
              <span matSuffix>&nbsp;EUR</span>
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>{{ 'maintenance.odometer' | translate }}</mat-label>
              <input matInput [(ngModel)]="form.odometerKm" type="number" />
              <span matSuffix>&nbsp;km</span>
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>{{ 'maintenance.performed_by' | translate }}</mat-label>
              <input matInput [(ngModel)]="form.performedBy" />
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>{{ 'maintenance.next_due_date' | translate }}</mat-label>
              <input matInput [(ngModel)]="form.nextDueDate" type="date" />
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>{{ 'maintenance.next_due_km' | translate }}</mat-label>
              <input matInput [(ngModel)]="form.nextDueKm" type="number" />
              <span matSuffix>&nbsp;km</span>
            </mat-form-field>
          </div>
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>{{ 'maintenance.description' | translate }}</mat-label>
            <textarea matInput [(ngModel)]="form.description" rows="2"></textarea>
          </mat-form-field>
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>{{ 'maintenance.notes' | translate }}</mat-label>
            <textarea matInput [(ngModel)]="form.notes" rows="2"></textarea>
          </mat-form-field>
          <button mat-raised-button color="primary" (click)="save()" [disabled]="!form.vehicleId || !form.type || !form.performedAt">
            <mat-icon>save</mat-icon> {{ (editingId ? 'vehicles.update' : 'vehicles.add_btn') | translate }}
          </button>
        </mat-card>
      }

      @if (overdueRecords.length > 0) {
        <mat-card class="alert-card">
          <mat-icon>warning</mat-icon>
          <span>{{ 'maintenance.overdue_alert' | translate:{ count: overdueRecords.length } }}</span>
        </mat-card>
      }

      <table mat-table [dataSource]="records" class="full-width">
        <ng-container matColumnDef="vehicleName">
          <th mat-header-cell *matHeaderCellDef>{{ 'maintenance.vehicle' | translate }}</th>
          <td mat-cell *matCellDef="let r">{{ r.vehicleName }}</td>
        </ng-container>
        <ng-container matColumnDef="type">
          <th mat-header-cell *matHeaderCellDef>{{ 'maintenance.type' | translate }}</th>
          <td mat-cell *matCellDef="let r">{{ ('maintenance.type_' + r.type.toLowerCase()) | translate }}</td>
        </ng-container>
        <ng-container matColumnDef="performedAt">
          <th mat-header-cell *matHeaderCellDef>{{ 'maintenance.date' | translate }}</th>
          <td mat-cell *matCellDef="let r">{{ r.performedAt }}</td>
        </ng-container>
        <ng-container matColumnDef="cost">
          <th mat-header-cell *matHeaderCellDef>{{ 'maintenance.cost' | translate }}</th>
          <td mat-cell *matCellDef="let r">{{ r.cost | number:'1.2-2' }} EUR</td>
        </ng-container>
        <ng-container matColumnDef="odometerKm">
          <th mat-header-cell *matHeaderCellDef>{{ 'maintenance.odometer' | translate }}</th>
          <td mat-cell *matCellDef="let r">{{ r.odometerKm ? (r.odometerKm | number) + ' km' : '-' }}</td>
        </ng-container>
        <ng-container matColumnDef="nextDueDate">
          <th mat-header-cell *matHeaderCellDef>{{ 'maintenance.next_due_date' | translate }}</th>
          <td mat-cell *matCellDef="let r" [class.overdue]="isOverdue(r.nextDueDate)">
            {{ r.nextDueDate || '-' }}
            @if (isOverdue(r.nextDueDate)) { <mat-icon class="warn-icon">error</mat-icon> }
          </td>
        </ng-container>
        <ng-container matColumnDef="actions">
          <th mat-header-cell *matHeaderCellDef>{{ 'vehicles.actions' | translate }}</th>
          <td mat-cell *matCellDef="let r">
            <button mat-icon-button (click)="edit(r)"><mat-icon>edit</mat-icon></button>
            <button mat-icon-button color="warn" (click)="deleteRecord(r.id)"><mat-icon>delete</mat-icon></button>
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
    .alert-card { display: flex; align-items: center; gap: 12px; padding: 16px; margin-bottom: 16px; background: #fff3e0; color: #e65100; }
    .overdue { color: #c62828; font-weight: 500; }
    .warn-icon { font-size: 16px; width: 16px; height: 16px; vertical-align: middle; margin-left: 4px; color: #c62828; }
    @media (max-width: 768px) {
      .page-container { padding: 12px; }
      table { display: block; overflow-x: auto; }
    }
  `]
})
export class MaintenanceComponent implements OnInit {
  private maintenanceService = inject(MaintenanceService);
  private http = inject(HttpClient);

  records: MaintenanceRecord[] = [];
  vehicles: VehicleOption[] = [];
  overdueRecords: MaintenanceRecord[] = [];
  showForm = false;
  editingId: string | null = null;
  form: MaintenanceRecord = this.emptyForm();
  columns = ['vehicleName', 'type', 'performedAt', 'cost', 'odometerKm', 'nextDueDate', 'actions'];

  ngOnInit(): void {
    this.load();
    this.http.get<any[]>(`/api/vehicles`).subscribe(v => this.vehicles = v.map(x => ({ id: x.id, name: x.name })));
  }

  load(): void {
    this.maintenanceService.getAll().subscribe(r => {
      this.records = r;
      this.overdueRecords = r.filter(rec => this.isOverdue(rec.nextDueDate));
    });
  }

  emptyForm(): MaintenanceRecord {
    return { vehicleId: '', type: '', performedAt: new Date().toISOString().split('T')[0], description: '', cost: 0, odometerKm: undefined, nextDueDate: '', nextDueKm: undefined, performedBy: '', notes: '' };
  }

  resetForm(): void { this.form = this.emptyForm(); this.editingId = null; }

  save(): void {
    if (this.editingId) {
      this.maintenanceService.update(this.editingId, this.form).subscribe(() => { this.load(); this.showForm = false; this.resetForm(); });
    } else {
      this.maintenanceService.create(this.form).subscribe(() => { this.load(); this.showForm = false; this.resetForm(); });
    }
  }

  edit(r: MaintenanceRecord): void { this.form = { ...r }; this.editingId = r.id!; this.showForm = true; }

  deleteRecord(id: string): void { this.maintenanceService.delete(id).subscribe(() => this.load()); }

  isOverdue(date?: string): boolean {
    if (!date) return false;
    return new Date(date) < new Date();
  }
}
