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
import { DocumentService, VehicleDoc } from '../shared/services/document.service';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-documents',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatCardModule, MatTableModule, MatButtonModule,
    MatIconModule, MatFormFieldModule, MatInputModule, MatSelectModule, TranslateModule
  ],
  template: `
    <div class="page-container">
      <div class="page-header">
        <h1>{{ 'documents.title' | translate }}</h1>
        <button mat-raised-button color="primary" (click)="showForm = !showForm; resetForm()">
          <mat-icon>{{ showForm ? 'close' : 'add' }}</mat-icon>
          {{ (showForm ? 'vehicles.cancel' : 'documents.add') | translate }}
        </button>
      </div>

      @if (showForm) {
        <mat-card class="form-card">
          <div class="form-grid">
            <mat-form-field appearance="outline">
              <mat-label>{{ 'documents.name' | translate }}</mat-label>
              <input matInput [(ngModel)]="form.name" required />
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>{{ 'documents.type' | translate }}</mat-label>
              <mat-select [(ngModel)]="form.type" required>
                <mat-option value="INSURANCE">{{ 'documents.type_insurance' | translate }}</mat-option>
                <mat-option value="INSPECTION">{{ 'documents.type_inspection' | translate }}</mat-option>
                <mat-option value="LICENSE">{{ 'documents.type_license' | translate }}</mat-option>
                <mat-option value="CMR">CMR</mat-option>
                <mat-option value="ADR">ADR</mat-option>
                <mat-option value="PERMIT">{{ 'documents.type_permit' | translate }}</mat-option>
                <mat-option value="OTHER">{{ 'documents.type_other' | translate }}</mat-option>
              </mat-select>
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>{{ 'documents.vehicle' | translate }}</mat-label>
              <mat-select [(ngModel)]="form.vehicleId">
                <mat-option [value]="null">-</mat-option>
                @for (v of vehicles; track v.id) {
                  <mat-option [value]="v.id">{{ v.name }}</mat-option>
                }
              </mat-select>
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>{{ 'documents.driver' | translate }}</mat-label>
              <mat-select [(ngModel)]="form.driverId">
                <mat-option [value]="null">-</mat-option>
                @for (d of drivers; track d.id) {
                  <mat-option [value]="d.id">{{ d.name }}</mat-option>
                }
              </mat-select>
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>{{ 'documents.doc_number' | translate }}</mat-label>
              <input matInput [(ngModel)]="form.documentNumber" />
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>{{ 'documents.issue_date' | translate }}</mat-label>
              <input matInput [(ngModel)]="form.issueDate" type="date" />
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>{{ 'documents.expiry_date' | translate }}</mat-label>
              <input matInput [(ngModel)]="form.expiryDate" type="date" />
            </mat-form-field>
          </div>
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>{{ 'documents.notes' | translate }}</mat-label>
            <textarea matInput [(ngModel)]="form.notes" rows="2"></textarea>
          </mat-form-field>
          <button mat-raised-button color="primary" (click)="save()" [disabled]="!form.name || !form.type">
            <mat-icon>save</mat-icon> {{ (editingId ? 'vehicles.update' : 'vehicles.add_btn') | translate }}
          </button>
        </mat-card>
      }

      @if (expiringDocs.length > 0) {
        <mat-card class="alert-card">
          <mat-icon>warning</mat-icon>
          <span>{{ 'documents.expiry_alert' | translate:{ count: expiringDocs.length } }}</span>
        </mat-card>
      }

      <table mat-table [dataSource]="docs" class="full-width">
        <ng-container matColumnDef="name">
          <th mat-header-cell *matHeaderCellDef>{{ 'documents.name' | translate }}</th>
          <td mat-cell *matCellDef="let d">{{ d.name }}</td>
        </ng-container>
        <ng-container matColumnDef="type">
          <th mat-header-cell *matHeaderCellDef>{{ 'documents.type' | translate }}</th>
          <td mat-cell *matCellDef="let d">{{ d.type }}</td>
        </ng-container>
        <ng-container matColumnDef="vehicleName">
          <th mat-header-cell *matHeaderCellDef>{{ 'documents.vehicle' | translate }}</th>
          <td mat-cell *matCellDef="let d">{{ d.vehicleName || '-' }}</td>
        </ng-container>
        <ng-container matColumnDef="driverName">
          <th mat-header-cell *matHeaderCellDef>{{ 'documents.driver' | translate }}</th>
          <td mat-cell *matCellDef="let d">{{ d.driverName || '-' }}</td>
        </ng-container>
        <ng-container matColumnDef="documentNumber">
          <th mat-header-cell *matHeaderCellDef>{{ 'documents.doc_number' | translate }}</th>
          <td mat-cell *matCellDef="let d">{{ d.documentNumber || '-' }}</td>
        </ng-container>
        <ng-container matColumnDef="expiryDate">
          <th mat-header-cell *matHeaderCellDef>{{ 'documents.expiry_date' | translate }}</th>
          <td mat-cell *matCellDef="let d" [class.expired]="isExpired(d.expiryDate)" [class.expiring-soon]="isExpiringSoon(d.expiryDate)">
            {{ d.expiryDate || '-' }}
            @if (isExpired(d.expiryDate)) { <mat-icon class="warn-icon">error</mat-icon> }
            @else if (isExpiringSoon(d.expiryDate)) { <mat-icon class="warn-icon">warning</mat-icon> }
          </td>
        </ng-container>
        <ng-container matColumnDef="actions">
          <th mat-header-cell *matHeaderCellDef>{{ 'vehicles.actions' | translate }}</th>
          <td mat-cell *matCellDef="let d">
            <button mat-icon-button (click)="edit(d)"><mat-icon>edit</mat-icon></button>
            <button mat-icon-button color="warn" (click)="deleteDoc(d.id)"><mat-icon>delete</mat-icon></button>
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
    .expired { color: #c62828; font-weight: 500; }
    .expiring-soon { color: #e65100; }
    .warn-icon { font-size: 16px; width: 16px; height: 16px; vertical-align: middle; margin-left: 4px; }
    @media (max-width: 768px) {
      .page-container { padding: 12px; }
      table { display: block; overflow-x: auto; }
    }
  `]
})
export class DocumentsComponent implements OnInit {
  private documentService = inject(DocumentService);
  private http = inject(HttpClient);

  docs: VehicleDoc[] = [];
  expiringDocs: VehicleDoc[] = [];
  vehicles: { id: string; name: string }[] = [];
  drivers: { id: string; name: string }[] = [];
  showForm = false;
  editingId: string | null = null;
  form: VehicleDoc = this.emptyForm();
  columns = ['name', 'type', 'vehicleName', 'driverName', 'documentNumber', 'expiryDate', 'actions'];

  ngOnInit(): void {
    this.load();
    this.http.get<any[]>(`/api/vehicles`).subscribe(v => this.vehicles = v.map(x => ({ id: x.id, name: x.name })));
    this.http.get<any[]>(`/api/drivers/active`).subscribe(d => this.drivers = d.map(x => ({ id: x.id, name: x.firstName + ' ' + x.lastName })));
  }

  load(): void {
    this.documentService.getAll().subscribe(d => {
      this.docs = d;
      this.expiringDocs = d.filter(doc => this.isExpired(doc.expiryDate) || this.isExpiringSoon(doc.expiryDate));
    });
  }

  emptyForm(): VehicleDoc { return { name: '', type: '', vehicleId: undefined, driverId: undefined, documentNumber: '', issueDate: '', expiryDate: '', notes: '' }; }
  resetForm(): void { this.form = this.emptyForm(); this.editingId = null; }

  save(): void {
    if (this.editingId) {
      this.documentService.update(this.editingId, this.form).subscribe(() => { this.load(); this.showForm = false; this.resetForm(); });
    } else {
      this.documentService.create(this.form).subscribe(() => { this.load(); this.showForm = false; this.resetForm(); });
    }
  }

  edit(d: VehicleDoc): void { this.form = { ...d }; this.editingId = d.id!; this.showForm = true; }
  deleteDoc(id: string): void { this.documentService.delete(id).subscribe(() => this.load()); }

  isExpiringSoon(date?: string): boolean {
    if (!date) return false;
    const d = new Date(date), now = new Date();
    const diff = (d.getTime() - now.getTime()) / (1000 * 60 * 60 * 24);
    return diff > 0 && diff <= 30;
  }

  isExpired(date?: string): boolean {
    if (!date) return false;
    return new Date(date) < new Date();
  }
}
