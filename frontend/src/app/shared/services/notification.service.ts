import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

export interface AppNotification {
  id: string;
  type: string;
  title: string;
  message: string;
  entityType?: string;
  entityId?: string;
  read: boolean;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private http = inject(HttpClient);
  private api = `${environment.apiUrl}/notifications`;

  unreadCount$ = new BehaviorSubject<number>(0);

  getAll(): Observable<AppNotification[]> { return this.http.get<AppNotification[]>(this.api); }
  getUnread(): Observable<AppNotification[]> { return this.http.get<AppNotification[]>(`${this.api}/unread`); }

  refreshUnreadCount(): void {
    this.http.get<{ count: number }>(`${this.api}/unread/count`).subscribe(
      res => this.unreadCount$.next(res.count)
    );
  }

  markAsRead(id: string): Observable<void> {
    return this.http.patch<void>(`${this.api}/${id}/read`, {}).pipe(
      tap(() => this.refreshUnreadCount())
    );
  }

  markAllAsRead(): Observable<void> {
    return this.http.patch<void>(`${this.api}/read-all`, {}).pipe(
      tap(() => this.unreadCount$.next(0))
    );
  }
}
