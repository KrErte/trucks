import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface LaneStats {
  origin: string;
  destination: string;
  tripCount: number;
  avgProfit: number;
  avgMargin: number;
  totalRevenue: number;
  totalCost: number;
}

export interface CostTrend {
  month: string;
  avgFuelCost: number;
  avgTollCost: number;
  avgDriverCost: number;
  avgTotalCost: number;
  avgRevenue: number;
  avgProfit: number;
  tripCount: number;
}

@Injectable({ providedIn: 'root' })
export class AnalyticsService {
  private http = inject(HttpClient);

  getLaneStats(): Observable<LaneStats[]> {
    return this.http.get<LaneStats[]>('/api/analytics/lanes');
  }

  getCostTrends(months: number = 12): Observable<CostTrend[]> {
    return this.http.get<CostTrend[]>(`/api/analytics/cost-trends?months=${months}`);
  }

  exportReport(month: string): Observable<Blob> {
    return this.http.get(`/api/analytics/report/export?month=${month}`, { responseType: 'blob' });
  }
}
