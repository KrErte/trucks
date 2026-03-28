import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Customer {
  id?: string;
  name: string;
  vatNumber?: string;
  regCode?: string;
  email?: string;
  phone?: string;
  address?: string;
  country?: string;
  contactPerson?: string;
  paymentTermDays?: number;
  active?: boolean;
  notes?: string;
}

@Injectable({ providedIn: 'root' })
export class CustomerService {
  private http = inject(HttpClient);
  private api = '/api/customers';

  getAll(): Observable<Customer[]> { return this.http.get<Customer[]>(this.api); }
  getActive(): Observable<Customer[]> { return this.http.get<Customer[]>(`${this.api}/active`); }
  create(customer: Customer): Observable<Customer> { return this.http.post<Customer>(this.api, customer); }
  update(id: string, customer: Customer): Observable<Customer> { return this.http.put<Customer>(`${this.api}/${id}`, customer); }
  delete(id: string): Observable<void> { return this.http.delete<void>(`${this.api}/${id}`); }
}
