import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface UserListItem {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  active: boolean;
  createdAt: string;
}

export interface InviteRequest {
  email: string;
  role: string;
}

export interface AuditLogItem {
  id: string;
  userId: string;
  action: string;
  entityType: string;
  entityId: string;
  details: string;
  ipAddress: string;
  createdAt: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

@Injectable({ providedIn: 'root' })
export class UserManagementService {
  private http = inject(HttpClient);

  getUsers(): Observable<UserListItem[]> {
    return this.http.get<UserListItem[]>('/api/users');
  }

  invite(req: InviteRequest): Observable<any> {
    return this.http.post('/api/users/invite', req);
  }

  acceptInvite(data: { token: string; firstName: string; lastName: string; password: string }): Observable<any> {
    return this.http.post('/api/users/invite/accept', data);
  }

  deactivateUser(id: string): Observable<UserListItem> {
    return this.http.put<UserListItem>(`/api/users/${id}/deactivate`, {});
  }

  reactivateUser(id: string): Observable<UserListItem> {
    return this.http.put<UserListItem>(`/api/users/${id}/reactivate`, {});
  }

  changeRole(id: string, role: string): Observable<UserListItem> {
    return this.http.put<UserListItem>(`/api/users/${id}/role`, { role });
  }

  getAuditLogs(page: number = 0, size: number = 50): Observable<PageResponse<AuditLogItem>> {
    return this.http.get<PageResponse<AuditLogItem>>(`/api/audit-logs?page=${page}&size=${size}`);
  }
}
