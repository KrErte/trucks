import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { TranslateModule } from '@ngx-translate/core';
import { UserManagementService } from '../shared/services/user-management.service';

@Component({
  selector: 'app-accept-invite',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatCardModule, MatFormFieldModule,
    MatInputModule, MatButtonModule, TranslateModule
  ],
  template: `
    <div class="accept-container">
      <mat-card class="accept-card">
        <mat-card-header>
          <mat-card-title>{{ 'users.accept_title' | translate }}</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          @if (success) {
            <p class="success">{{ 'users.accept_success' | translate }}</p>
            <button mat-raised-button color="primary" routerLink="/login">{{ 'login.submit' | translate }}</button>
          } @else {
            <form [formGroup]="form" (ngSubmit)="accept()">
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>{{ 'users.first_name' | translate }}</mat-label>
                <input matInput formControlName="firstName" />
              </mat-form-field>
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>{{ 'users.last_name' | translate }}</mat-label>
                <input matInput formControlName="lastName" />
              </mat-form-field>
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>{{ 'users.password' | translate }}</mat-label>
                <input matInput formControlName="password" type="password" />
              </mat-form-field>
              @if (errorMessage) {
                <p class="error">{{ errorMessage }}</p>
              }
              <button mat-raised-button color="primary" type="submit" [disabled]="form.invalid || loading">
                {{ 'users.accept_btn' | translate }}
              </button>
            </form>
          }
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .accept-container { display: flex; justify-content: center; align-items: center; min-height: 80vh; padding: 24px; }
    .accept-card { max-width: 400px; width: 100%; }
    .full-width { width: 100%; }
    .success { color: #2e7d32; }
    .error { color: #c62828; }
  `]
})
export class AcceptInviteComponent implements OnInit {
  private fb = inject(FormBuilder);
  private route = inject(ActivatedRoute);
  private userService = inject(UserManagementService);

  token = '';
  loading = false;
  success = false;
  errorMessage = '';

  form = this.fb.group({
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    password: ['', [Validators.required, Validators.minLength(6)]]
  });

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token') || '';
  }

  accept(): void {
    if (this.form.invalid || !this.token) return;
    this.loading = true;
    this.userService.acceptInvite({
      token: this.token,
      ...this.form.value as any
    }).subscribe({
      next: () => {
        this.success = true;
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to accept invitation';
        this.loading = false;
      }
    });
  }
}
