import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface VehicleDoc {
  id?: string;
  vehicleId?: string;
  vehicleName?: string;
  driverId?: string;
  driverName?: string;
  type: string;
  name: string;
  expiryDate?: string;
  issueDate?: string;
  documentNumber?: string;
  notes?: string;
}

@Injectable({ providedIn: 'root' })
export class DocumentService {
  private http = inject(HttpClient);
  private api = '/api/documents';

  getAll(): Observable<VehicleDoc[]> { return this.http.get<VehicleDoc[]>(this.api); }
  create(doc: VehicleDoc): Observable<VehicleDoc> { return this.http.post<VehicleDoc>(this.api, doc); }
  update(id: string, doc: VehicleDoc): Observable<VehicleDoc> { return this.http.put<VehicleDoc>(`${this.api}/${id}`, doc); }
  delete(id: string): Observable<void> { return this.http.delete<void>(`${this.api}/${id}`); }
}
