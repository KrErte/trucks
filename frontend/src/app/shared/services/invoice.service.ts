import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Invoice {
  id: string;
  invoiceNumber: string;
  customerName: string;
  customerVat: string;
  amount: number;
  currency: string;
  issuedDate: string;
  dueDate: string;
  status: string;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class InvoiceService {
  private http = inject(HttpClient);

  getInvoices(): Observable<Invoice[]> {
    return this.http.get<Invoice[]>('/api/invoices');
  }

  createInvoice(calculationId: string, customerName: string, customerVat: string, dueDays: number): Observable<Invoice> {
    return this.http.post<Invoice>('/api/invoices', { calculationId, customerName, customerVat, dueDays });
  }

  updateStatus(id: string, status: string): Observable<Invoice> {
    return this.http.put<Invoice>(`/api/invoices/${id}/status`, { status });
  }

  exportCSV(): Observable<Blob> {
    return this.http.get('/api/invoices/export/csv', { responseType: 'blob' });
  }

  exportXML(): Observable<Blob> {
    return this.http.get('/api/invoices/export/xml', { responseType: 'blob' });
  }
}
