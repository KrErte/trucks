import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ScrapedTruckData, Vehicle } from '../models/vehicle.model';

@Injectable({ providedIn: 'root' })
export class ScraperService {
  private http = inject(HttpClient);
  private apiUrl = '/api/scraper/truck1';

  searchTrucks(brand: string, type: string, page: number = 1): Observable<ScrapedTruckData[]> {
    const params = new HttpParams()
      .set('brand', brand)
      .set('type', type)
      .set('page', page.toString());
    return this.http.get<ScrapedTruckData[]>(`${this.apiUrl}/search`, { params });
  }

  getTruckDetails(url: string): Observable<ScrapedTruckData> {
    return this.http.get<ScrapedTruckData>(`${this.apiUrl}/details`, {
      params: new HttpParams().set('url', url)
    });
  }

  importTruck(data: ScrapedTruckData): Observable<Vehicle> {
    return this.http.post<Vehicle>(`${this.apiUrl}/import`, data);
  }
}
