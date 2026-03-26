import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface FuelPrice {
  id: string;
  countryCode: string;
  fuelType: string;
  pricePerLiter: number;
  validFrom: string;
  source: string;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class FuelPriceService {
  private http = inject(HttpClient);

  getAll(): Observable<FuelPrice[]> {
    return this.http.get<FuelPrice[]>('/api/fuel-prices');
  }

  getByCountry(country: string): Observable<FuelPrice[]> {
    return this.http.get<FuelPrice[]>(`/api/fuel-prices/${country}`);
  }

  refresh(): Observable<{ message: string; updatedCount: number }> {
    return this.http.post<{ message: string; updatedCount: number }>('/api/fuel-prices/refresh', {});
  }
}
