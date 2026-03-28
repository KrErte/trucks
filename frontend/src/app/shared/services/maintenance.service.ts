import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface MaintenanceRecord {
  id?: string;
  vehicleId: string;
  vehicleName?: string;
  type: string;
  description?: string;
  cost?: number;
  odometerKm?: number;
  performedAt: string;
  nextDueDate?: string;
  nextDueKm?: number;
  performedBy?: string;
  notes?: string;
}

@Injectable({ providedIn: 'root' })
export class MaintenanceService {
  private http = inject(HttpClient);
  private api = `${environment.apiUrl}/maintenance`;

  getAll(): Observable<MaintenanceRecord[]> { return this.http.get<MaintenanceRecord[]>(this.api); }
  getByVehicle(vehicleId: string): Observable<MaintenanceRecord[]> { return this.http.get<MaintenanceRecord[]>(`${this.api}/vehicle/${vehicleId}`); }
  create(record: MaintenanceRecord): Observable<MaintenanceRecord> { return this.http.post<MaintenanceRecord>(this.api, record); }
  update(id: string, record: MaintenanceRecord): Observable<MaintenanceRecord> { return this.http.put<MaintenanceRecord>(`${this.api}/${id}`, record); }
  delete(id: string): Observable<void> { return this.http.delete<void>(`${this.api}/${id}`); }
}
