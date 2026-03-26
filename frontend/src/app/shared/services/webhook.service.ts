import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface WebhookEndpoint {
  id: string;
  url: string;
  secret: string;
  events: string[];
  active: boolean;
  createdAt: string;
}

export interface WebhookDelivery {
  id: string;
  eventType: string;
  payload: string;
  responseStatus: number;
  failed: boolean;
  retryCount: number;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class WebhookService {
  private http = inject(HttpClient);

  getEndpoints(): Observable<WebhookEndpoint[]> {
    return this.http.get<WebhookEndpoint[]>('/api/webhooks');
  }

  createEndpoint(url: string, events: string[]): Observable<WebhookEndpoint> {
    return this.http.post<WebhookEndpoint>('/api/webhooks', { url, events });
  }

  deleteEndpoint(id: string): Observable<void> {
    return this.http.delete<void>(`/api/webhooks/${id}`);
  }

  toggleEndpoint(id: string): Observable<WebhookEndpoint> {
    return this.http.put<WebhookEndpoint>(`/api/webhooks/${id}/toggle`, {});
  }

  getDeliveries(endpointId: string, page = 0, size = 20): Observable<any> {
    return this.http.get(`/api/webhooks/${endpointId}/deliveries?page=${page}&size=${size}`);
  }
}
