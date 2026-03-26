import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CalculateRequest, CalculationResponse, PageResponse, AlternativesResponse } from '../models/calculation.model';

@Injectable({ providedIn: 'root' })
export class CalculationService {
  private http = inject(HttpClient);
  private apiUrl = '/api/calculations';

  calculate(req: CalculateRequest): Observable<CalculationResponse> {
    return this.http.post<CalculationResponse>('/api/calculate', req);
  }

  previewAlternatives(req: CalculateRequest): Observable<AlternativesResponse> {
    return this.http.post<AlternativesResponse>('/api/calculations/preview', req);
  }

  getHistory(page: number = 0, size: number = 20): Observable<PageResponse<CalculationResponse>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PageResponse<CalculationResponse>>(this.apiUrl, { params });
  }

  getCalculation(id: string): Observable<CalculationResponse> {
    return this.http.get<CalculationResponse>(`${this.apiUrl}/${id}`);
  }
}
