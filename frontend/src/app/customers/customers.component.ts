import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { TranslateModule } from '@ngx-translate/core';
import { CustomerService, Customer } from '../shared/services/customer.service';

@Component({
  selector: 'app-customers',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatCardModule, MatTableModule, MatButtonModule,
    MatIconModule, MatFormFieldModule, MatInputModule, TranslateModule
  ],
  template: `
    <div class="page-container">
      <div class="page-header">
        <h1>{{ 'customers.title' | translate }}</h1>
        <button mat-raised-button color="primary" (click)="showForm = !showForm; resetForm()">
          <mat-icon>{{ showForm ? 'close' : 'person_add' }}</mat-icon>
          {{ (showForm ? 'vehicles.cancel' : 'customers.add') | translate }}
        </button>
      </div>

      @if (showForm) {
        <mat-card class="form-card">
          <div class="form-grid">
            <mat-form-field appearance="outline">
              <mat-label>{{ 'customers.name' | translate }}</mat-label>
              <input matInput [(ngModel)]="form.name" required />
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>{{ 'customers.reg_code' | translate }}</mat-label>
              <input matInput [(ngModel)]="form.regCode" />
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>{{ 'customers.vat_number' | translate }}</mat-label>
              <input matInput [(ngModel)]="form.vatNumber" />
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>{{ 'customers.email' | translate }}</mat-label>
              <input matInput [(ngModel)]="form.email" type="email" />
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>{{ 'customers.phone' | translate }}</mat-label>
              <input matInput [(ngModel)]="form.phone" />
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>{{ 'customers.contact_person' | translate }}</mat-label>
              <input matInput [(ngModel)]="form.contactPerson" />
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>{{ 'customers.country' | translate }}</mat-label>
              <input matInput [(ngModel)]="form.country" />
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>{{ 'customers.payment_terms' | translate }}</mat-label>
              <input matInput [(ngModel)]="form.paymentTermDays" type="number" />
              <span matSuffix>&nbsp;{{ 'customers.days' | translate }}</span>
            </mat-form-field>
          </div>
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>{{ 'customers.address' | translate }}</mat-label>
            <textarea matInput [(ngModel)]="form.address" rows="2"></textarea>
          </mat-form-field>
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>{{ 'customers.notes' | translate }}</mat-label>
            <textarea matInput [(ngModel)]="form.notes" rows="2"></textarea>
          </mat-form-field>
          <button mat-raised-button color="primary" (click)="save()" [disabled]="!form.name">
            <mat-icon>save</mat-icon> {{ (editingId ? 'vehicles.update' : 'vehicles.add_btn') | translate }}
          </button>
        </mat-card>
      }

      <table mat-table [dataSource]="customers" class="full-width">
        <ng-container matColumnDef="name">
          <th mat-header-cell *matHeaderCellDef>{{ 'customers.name' | translate }}</th>
          <td mat-cell *matCellDef="let c">{{ c.name }}</td>
        </ng-container>
        <ng-container matColumnDef="regCode">
          <th mat-header-cell *matHeaderCellDef>{{ 'customers.reg_code' | translate }}</th>
          <td mat-cell *matCellDef="let c">{{ c.regCode }}</td>
        </ng-container>
        <ng-container matColumnDef="email">
          <th mat-header-cell *matHeaderCellDef>{{ 'customers.email' | translate }}</th>
          <td mat-cell *matCellDef="let c">{{ c.email }}</td>
        </ng-container>
        <ng-container matColumnDef="phone">
          <th mat-header-cell *matHeaderCellDef>{{ 'customers.phone' | translate }}</th>
          <td mat-cell *matCellDef="let c">{{ c.phone }}</td>
        </ng-container>
        <ng-container matColumnDef="contactPerson">
          <th mat-header-cell *matHeaderCellDef>{{ 'customers.contact_person' | translate }}</th>
          <td mat-cell *matCellDef="let c">{{ c.contactPerson }}</td>
        </ng-container>
        <ng-container matColumnDef="paymentTermDays">
          <th mat-header-cell *matHeaderCellDef>{{ 'customers.payment_terms' | translate }}</th>
          <td mat-cell *matCellDef="let c">{{ c.paymentTermDays }} {{ 'customers.days' | translate }}</td>
        </ng-container>
        <ng-container matColumnDef="actions">
          <th mat-header-cell *matHeaderCellDef>{{ 'vehicles.actions' | translate }}</th>
          <td mat-cell *matCellDef="let c">
            <button mat-icon-button (click)="edit(c)"><mat-icon>edit</mat-icon></button>
            <button mat-icon-button color="warn" (click)="deleteCustomer(c.id)"><mat-icon>delete</mat-icon></button>
          </td>
        </ng-container>

        <tr mat-header-row *matHeaderRowDef="columns"></tr>
        <tr mat-row *matRowDef="let row; columns: columns;"></tr>
      </table>

      @if (customers.length === 0) {
        <div class="empty-state">
          <mat-icon>people</mat-icon>
          <p>{{ 'customers.empty' | translate }}</p>
        </div>
      }
    </div>
  `,
  styles: [`
    .page-container { padding: 24px; max-width: 1200px; margin: 0 auto; }
    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
    .page-header h1 { margin: 0; }
    .form-card { padding: 24px; margin-bottom: 24px; }
    .form-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 12px; margin-bottom: 16px; }
    .full-width { width: 100%; }
    .empty-state { text-align: center; padding: 48px; color: #888; }
    .empty-state mat-icon { font-size: 48px; width: 48px; height: 48px; margin-bottom: 16px; }
    @media (max-width: 768px) {
      .page-container { padding: 12px; }
      table { display: block; overflow-x: auto; }
    }
  `]
})
export class CustomersComponent implements OnInit {
  private customerService = inject(CustomerService);

  customers: Customer[] = [];
  showForm = false;
  editingId: string | null = null;
  form: Customer = this.emptyForm();
  columns = ['name', 'regCode', 'email', 'phone', 'contactPerson', 'paymentTermDays', 'actions'];

  ngOnInit(): void { this.load(); }

  load(): void { this.customerService.getAll().subscribe(c => this.customers = c); }

  emptyForm(): Customer {
    return { name: '', vatNumber: '', regCode: '', email: '', phone: '', address: '', country: '', contactPerson: '', paymentTermDays: 14, notes: '' };
  }

  resetForm(): void { this.form = this.emptyForm(); this.editingId = null; }

  save(): void {
    if (this.editingId) {
      this.customerService.update(this.editingId, this.form).subscribe(() => { this.load(); this.showForm = false; this.resetForm(); });
    } else {
      this.customerService.create(this.form).subscribe(() => { this.load(); this.showForm = false; this.resetForm(); });
    }
  }

  edit(c: Customer): void { this.form = { ...c }; this.editingId = c.id!; this.showForm = true; }

  deleteCustomer(id: string): void { this.customerService.delete(id).subscribe(() => this.load()); }
}
