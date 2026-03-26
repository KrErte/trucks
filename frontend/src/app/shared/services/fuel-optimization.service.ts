import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface FuelStopSuggestion {
  country: string;
  countryName: string;
  fuelPricePerLiter: number;
  litersToFill: number;
  cost: number;
  reason: string;
}

export interface FuelOptimizationResponse {
  stops: FuelStopSuggestion[];
  totalOptimizedCost: number;
  totalNaiveCost: number;
  savings: number;
  savingsPercent: number;
}

export interface FuelOptimizationRequest {
  vehicleId: string;
  originAddress: string;
  destinationAddress: string;
  distanceKm: number;
  currentFuelLiters?: number;
  isLoaded: boolean;
}

@Injectable({ providedIn: 'root' })
export class FuelOptimizationService {
  private http = inject(HttpClient);

  optimize(req: FuelOptimizationRequest): Observable<FuelOptimizationResponse> {
    return this.http.post<FuelOptimizationResponse>('/api/fuel-optimization', req);
  }
}
