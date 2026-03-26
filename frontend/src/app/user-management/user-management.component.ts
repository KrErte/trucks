import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTabsModule } from '@angular/material/tabs';
import { MatChipsModule } from '@angular/material/chips';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { TranslateModule } from '@ngx-translate/core';
import { UserManagementService, UserListItem, AuditLogItem } from '../shared/services/user-management.service';

@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatCardModule, MatTableModule,
    MatButtonModule, MatIconModule, MatFormFieldModule, MatInputModule,
    MatSelectModule, MatTabsModule, MatChipsModule, MatPaginatorModule,
    MatDialogModule, TranslateModule
  ],
  template: `
    <div class="user-management-container">
      <h1>{{ 'users.title' | translate }}</h1>

      <mat-tab-group>
        <mat-tab [label]="'users.tab_users' | translate">
          <div class="tab-content">
            <div class="actions-row">
              <button mat-raised-button color="primary" (click)="showInvite = !showInvite">
                <mat-icon>person_add</mat-icon> {{ 'users.invite' | translate }}
              </button>
            </div>

            @if (showInvite) {
              <mat-card class="invite-card">
                <mat-card-content>
                  <form [formGroup]="inviteForm" (ngSubmit)="sendInvite()">
                    <div class="form-row">
                      <mat-form-field appearance="outline">
                        <mat-label>{{ 'users.email' | translate }}</mat-label>
                        <input matInput formControlName="email" type="email" />
                      </mat-form-field>
                      <mat-form-field appearance="outline">
                        <mat-label>{{ 'users.role' | translate }}</mat-label>
                        <mat-select formControlName="role">
                          <mat-option value="USER">{{ 'users.role_user' | translate }}</mat-option>
                          <mat-option value="ADMIN">{{ 'users.role_admin' | translate }}</mat-option>
                          <mat-option value="DISPATCHER">{{ 'users.role_dispatcher' | translate }}</mat-option>
                          <mat-option value="DRIVER">{{ 'users.role_driver' | translate }}</mat-option>
                          <mat-option value="ACCOUNTANT">{{ 'users.role_accountant' | translate }}</mat-option>
                        </mat-select>
                      </mat-form-field>
                      <button mat-raised-button color="primary" type="submit" [disabled]="inviteForm.invalid">
                        {{ 'users.send_invite' | translate }}
                      </button>
                    </div>
                  </form>
                </mat-card-content>
              </mat-card>
            }

            @if (inviteSuccess) {
              <div class="success-message">{{ 'users.invite_sent' | translate }}</div>
            }

            <mat-card>
              <table mat-table [dataSource]="users" class="full-width">
                <ng-container matColumnDef="email">
                  <th mat-header-cell *matHeaderCellDef>{{ 'users.email' | translate }}</th>
                  <td mat-cell *matCellDef="let u">{{ u.email }}</td>
                </ng-container>
                <ng-container matColumnDef="name">
                  <th mat-header-cell *matHeaderCellDef>{{ 'users.name' | translate }}</th>
                  <td mat-cell *matCellDef="let u">{{ u.firstName }} {{ u.lastName }}</td>
                </ng-container>
                <ng-container matColumnDef="role">
                  <th mat-header-cell *matHeaderCellDef>{{ 'users.role' | translate }}</th>
                  <td mat-cell *matCellDef="let u">
                    <mat-form-field appearance="outline" class="role-select">
                      <mat-select [value]="u.role" (selectionChange)="onRoleChange(u.id, $event.value)">
                        <mat-option value="ADMIN">Admin</mat-option>
                        <mat-option value="USER">User</mat-option>
                        <mat-option value="DISPATCHER">Dispatcher</mat-option>
                        <mat-option value="DRIVER">Driver</mat-option>
                        <mat-option value="ACCOUNTANT">Accountant</mat-option>
                      </mat-select>
                    </mat-form-field>
                  </td>
                </ng-container>
                <ng-container matColumnDef="active">
                  <th mat-header-cell *matHeaderCellDef>{{ 'users.status' | translate }}</th>
                  <td mat-cell *matCellDef="let u">
                    <span [class.active-badge]="u.active" [class.inactive-badge]="!u.active">
                      {{ u.active ? ('users.active' | translate) : ('users.inactive' | translate) }}
                    </span>
                  </td>
                </ng-container>
                <ng-container matColumnDef="actions">
                  <th mat-header-cell *matHeaderCellDef>{{ 'users.actions' | translate }}</th>
                  <td mat-cell *matCellDef="let u">
                    @if (u.active) {
                      <button mat-icon-button color="warn" (click)="deactivate(u.id)" title="{{ 'users.deactivate' | translate }}">
                        <mat-icon>block</mat-icon>
                      </button>
                    } @else {
                      <button mat-icon-button color="primary" (click)="reactivate(u.id)" title="{{ 'users.reactivate' | translate }}">
                        <mat-icon>check_circle</mat-icon>
                      </button>
                    }
                  </td>
                </ng-container>

                <tr mat-header-row *matHeaderRowDef="userColumns"></tr>
                <tr mat-row *matRowDef="let row; columns: userColumns;"></tr>
              </table>
            </mat-card>
          </div>
        </mat-tab>

        <mat-tab [label]="'users.tab_audit' | translate">
          <div class="tab-content">
            <mat-card>
              <table mat-table [dataSource]="auditLogs" class="full-width">
                <ng-container matColumnDef="createdAt">
                  <th mat-header-cell *matHeaderCellDef>{{ 'users.audit_date' | translate }}</th>
                  <td mat-cell *matCellDef="let a">{{ a.createdAt | date:'dd.MM.yyyy HH:mm' }}</td>
                </ng-container>
                <ng-container matColumnDef="action">
                  <th mat-header-cell *matHeaderCellDef>{{ 'users.audit_action' | translate }}</th>
                  <td mat-cell *matCellDef="let a">{{ a.action }}</td>
                </ng-container>
                <ng-container matColumnDef="entityType">
                  <th mat-header-cell *matHeaderCellDef>{{ 'users.audit_entity' | translate }}</th>
                  <td mat-cell *matCellDef="let a">{{ a.entityType }}</td>
                </ng-container>
                <ng-container matColumnDef="details">
                  <th mat-header-cell *matHeaderCellDef>{{ 'users.audit_details' | translate }}</th>
                  <td mat-cell *matCellDef="let a">{{ a.details }}</td>
                </ng-container>

                <tr mat-header-row *matHeaderRowDef="auditColumns"></tr>
                <tr mat-row *matRowDef="let row; columns: auditColumns;"></tr>
              </table>

              <mat-paginator
                [length]="auditTotal"
                [pageSize]="50"
                (page)="onAuditPage($event)">
              </mat-paginator>
            </mat-card>
          </div>
        </mat-tab>
      </mat-tab-group>
    </div>
  `,
  styles: [`
    .user-management-container { padding: 24px; max-width: 1200px; margin: 0 auto; }
    .tab-content { padding: 16px 0; }
    .actions-row { margin-bottom: 16px; }
    .invite-card { margin-bottom: 16px; }
    .form-row { display: flex; gap: 16px; align-items: center; flex-wrap: wrap; }
    .form-row mat-form-field { flex: 1; min-width: 200px; }
    .role-select { width: 140px; }
    .full-width { width: 100%; }
    .success-message { color: #2e7d32; padding: 8px 16px; margin-bottom: 16px; background: #e8f5e9; border-radius: 4px; }
    .active-badge { color: #2e7d32; font-weight: 500; }
    .inactive-badge { color: #c62828; font-weight: 500; }
    @media (max-width: 768px) {
      .user-management-container { padding: 12px; }
      .form-row { flex-direction: column; }
    }
  `]
})
export class UserManagementComponent implements OnInit {
  private fb = inject(FormBuilder);
  private userService = inject(UserManagementService);

  users: UserListItem[] = [];
  auditLogs: AuditLogItem[] = [];
  auditTotal = 0;
  showInvite = false;
  inviteSuccess = false;
  userColumns = ['email', 'name', 'role', 'active', 'actions'];
  auditColumns = ['createdAt', 'action', 'entityType', 'details'];

  inviteForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    role: ['USER', Validators.required]
  });

  ngOnInit(): void {
    this.loadUsers();
    this.loadAuditLogs(0);
  }

  loadUsers(): void {
    this.userService.getUsers().subscribe(u => this.users = u);
  }

  loadAuditLogs(page: number): void {
    this.userService.getAuditLogs(page).subscribe(res => {
      this.auditLogs = res.content;
      this.auditTotal = res.totalElements;
    });
  }

  sendInvite(): void {
    if (this.inviteForm.invalid) return;
    this.userService.invite(this.inviteForm.value as any).subscribe({
      next: () => {
        this.inviteSuccess = true;
        this.showInvite = false;
        this.inviteForm.reset({ role: 'USER' });
        setTimeout(() => this.inviteSuccess = false, 3000);
      },
      error: (err) => alert(err.error?.message || 'Failed to send invite')
    });
  }

  onRoleChange(userId: string, role: string): void {
    this.userService.changeRole(userId, role).subscribe(() => this.loadUsers());
  }

  deactivate(id: string): void {
    this.userService.deactivateUser(id).subscribe(() => this.loadUsers());
  }

  reactivate(id: string): void {
    this.userService.reactivateUser(id).subscribe(() => this.loadUsers());
  }

  onAuditPage(event: PageEvent): void {
    this.loadAuditLogs(event.pageIndex);
  }
}
