import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface PricingSuggestRequest {
  originAddress: string;
  destinationAddress: string;
  vehicleId: string;
  cargoWeightTons?: number;
  includeReturnTrip?: boolean;
}

export interface SuggestedPriceRange {
  estimatedTotalCost: number;
  minPrice: number;
  optimalPrice: number;
  premiumPrice: number;
  distanceKm: number;
  costPerKm: number;
  minRevenuePerKm: number;
  optimalRevenuePerKm: number;
  premiumRevenuePerKm: number;
}

export interface MonthlyLaneStat {
  yearMonth: string;
  tripCount: number;
  avgPrice: number;
  avgMarginPct: number;
}

export interface LaneIntelligence {
  tripCount: number;
  avgPrice: number;
  avgMarginPct: number;
  avgRevenuePerKm: number;
  priceTrend: string;
  bestTripMarginPct: number;
  worstTripMarginPct: number;
  monthlyBreakdown: MonthlyLaneStat[];
  matchType: string;
}

export interface MarketContext {
  companyAvgMarginPct: number;
  companyAvgRevenuePerKm: number;
  laneVsCompanyMarginDelta: number;
  seasonalPattern: string;
}

export interface PricingSuggestionResponse {
  priceRange: SuggestedPriceRange;
  laneIntelligence: LaneIntelligence | null;
  marketContext: MarketContext;
}

@Injectable({ providedIn: 'root' })
export class PricingService {
  private http = inject(HttpClient);

  suggest(req: PricingSuggestRequest): Observable<PricingSuggestionResponse> {
    return this.http.post<PricingSuggestionResponse>('/api/pricing/suggest', req);
  }
}
