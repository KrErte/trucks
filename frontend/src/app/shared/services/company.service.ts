import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface CompanySettings {
  companyName: string;
  vatNumber: string;
  country: string;
  defaultDriverDailyRate: number | null;
}

@Injectable({ providedIn: 'root' })
export class CompanyService {
  private http = inject(HttpClient);
  private apiUrl = '/api/company';

  getSettings(): Observable<CompanySettings> {
    return this.http.get<CompanySettings>(this.apiUrl);
  }

  updateSettings(settings: CompanySettings): Observable<CompanySettings> {
    return this.http.put<CompanySettings>(this.apiUrl, settings);
  }
}
