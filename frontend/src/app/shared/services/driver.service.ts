import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Driver {
  id?: string;
  firstName: string;
  lastName: string;
  email?: string;
  phone?: string;
  licenseNumber?: string;
  licenseExpiry?: string;
  licenseCategories?: string;
  idCardNumber?: string;
  idCardExpiry?: string;
  adrCertificateExpiry?: string;
  driverCardNumber?: string;
  driverCardExpiry?: string;
  dailyRate?: number;
  active?: boolean;
  notes?: string;
}

@Injectable({ providedIn: 'root' })
export class DriverService {
  private http = inject(HttpClient);
  private api = '/api/drivers';

  getAll(): Observable<Driver[]> { return this.http.get<Driver[]>(this.api); }
  getActive(): Observable<Driver[]> { return this.http.get<Driver[]>(`${this.api}/active`); }
  create(driver: Driver): Observable<Driver> { return this.http.post<Driver>(this.api, driver); }
  update(id: string, driver: Driver): Observable<Driver> { return this.http.put<Driver>(`${this.api}/${id}`, driver); }
  delete(id: string): Observable<void> { return this.http.delete<void>(`${this.api}/${id}`); }
  deactivate(id: string): Observable<void> { return this.http.patch<void>(`${this.api}/${id}/deactivate`, {}); }
  activate(id: string): Observable<void> { return this.http.patch<void>(`${this.api}/${id}/activate`, {}); }
}
