import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { TranslateModule } from '@ngx-translate/core';
import { InvoiceService, Invoice } from '../shared/services/invoice.service';

@Component({
  selector: 'app-invoices',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatCardModule, MatTableModule, MatButtonModule,
    MatIconModule, MatChipsModule, MatSelectModule, MatFormFieldModule, TranslateModule
  ],
  template: `
    <div class="invoices-container" style="max-width: 1100px; margin: 0 auto; padding: 24px;">
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px;">
        <h1>{{ 'invoices.title' | translate }}</h1>
        <div style="display: flex; gap: 8px;">
          <button mat-raised-button (click)="exportCSV()">
            <mat-icon>download</mat-icon> e-Arveldaja CSV
          </button>
          <button mat-raised-button (click)="exportXML()">
            <mat-icon>download</mat-icon> Merit XML
          </button>
        </div>
      </div>

      <mat-card>
        <table mat-table [dataSource]="invoices" style="width: 100%;">
          <ng-container matColumnDef="invoiceNumber">
            <th mat-header-cell *matHeaderCellDef>{{ 'invoices.number' | translate }}</th>
            <td mat-cell *matCellDef="let i">{{ i.invoiceNumber }}</td>
          </ng-container>
          <ng-container matColumnDef="customerName">
            <th mat-header-cell *matHeaderCellDef>{{ 'invoices.customer' | translate }}</th>
            <td mat-cell *matCellDef="let i">{{ i.customerName }}</td>
          </ng-container>
          <ng-container matColumnDef="amount">
            <th mat-header-cell *matHeaderCellDef>{{ 'invoices.amount' | translate }}</th>
            <td mat-cell *matCellDef="let i">{{ i.amount | number:'1.2-2' }} {{ i.currency }}</td>
          </ng-container>
          <ng-container matColumnDef="issuedDate">
            <th mat-header-cell *matHeaderCellDef>{{ 'invoices.issued' | translate }}</th>
            <td mat-cell *matCellDef="let i">{{ i.issuedDate | date:'dd.MM.yyyy' }}</td>
          </ng-container>
          <ng-container matColumnDef="dueDate">
            <th mat-header-cell *matHeaderCellDef>{{ 'invoices.due_date' | translate }}</th>
            <td mat-cell *matCellDef="let i">{{ i.dueDate | date:'dd.MM.yyyy' }}</td>
          </ng-container>
          <ng-container matColumnDef="status">
            <th mat-header-cell *matHeaderCellDef>{{ 'invoices.status' | translate }}</th>
            <td mat-cell *matCellDef="let i">
              <mat-form-field appearance="outline" style="width: 120px; margin: 0;">
                <mat-select [value]="i.status" (selectionChange)="changeStatus(i.id, $event.value)">
                  <mat-option value="DRAFT">DRAFT</mat-option>
                  <mat-option value="SENT">SENT</mat-option>
                  <mat-option value="PAID">PAID</mat-option>
                  <mat-option value="OVERDUE">OVERDUE</mat-option>
                  <mat-option value="CANCELLED">CANCELLED</mat-option>
                </mat-select>
              </mat-form-field>
            </td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
          <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
        </table>
      </mat-card>

      @if (invoices.length === 0) {
        <mat-card style="padding: 48px; text-align: center; margin-top: 16px;">
          <mat-icon style="font-size: 48px; width: 48px; height: 48px; color: #ccc;">receipt_long</mat-icon>
          <p>{{ 'invoices.empty' | translate }}</p>
        </mat-card>
      }
    </div>
  `
})
export class InvoicesComponent implements OnInit {
  private invoiceService = inject(InvoiceService);

  invoices: Invoice[] = [];
  displayedColumns = ['invoiceNumber', 'customerName', 'amount', 'issuedDate', 'dueDate', 'status'];

  ngOnInit(): void {
    this.loadInvoices();
  }

  loadInvoices(): void {
    this.invoiceService.getInvoices().subscribe(res => this.invoices = res);
  }

  changeStatus(id: string, status: string): void {
    this.invoiceService.updateStatus(id, status).subscribe(updated => {
      const idx = this.invoices.findIndex(i => i.id === id);
      if (idx >= 0) this.invoices[idx] = updated;
    });
  }

  exportCSV(): void {
    this.invoiceService.exportCSV().subscribe(blob => {
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'invoices-earveldaja.csv';
      a.click();
      window.URL.revokeObjectURL(url);
    });
  }

  exportXML(): void {
    this.invoiceService.exportXML().subscribe(blob => {
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'invoices-merit.xml';
      a.click();
      window.URL.revokeObjectURL(url);
    });
  }
}
