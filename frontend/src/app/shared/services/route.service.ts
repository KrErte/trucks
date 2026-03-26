import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface SavedRoute {
  id: string;
  name: string;
  originAddress: string;
  destinationAddress: string;
}

@Injectable({ providedIn: 'root' })
export class RouteService {
  private http = inject(HttpClient);

  getRoutes(): Observable<SavedRoute[]> {
    return this.http.get<SavedRoute[]>('/api/saved-routes');
  }

  saveRoute(route: { name: string; originAddress: string; destinationAddress: string }): Observable<SavedRoute> {
    return this.http.post<SavedRoute>('/api/saved-routes', route);
  }

  deleteRoute(id: string): Observable<void> {
    return this.http.delete<void>(`/api/saved-routes/${id}`);
  }
}
