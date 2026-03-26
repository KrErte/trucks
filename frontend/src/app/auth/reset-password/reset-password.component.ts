import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, MatCardModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule, TranslateModule],
  template: `
    <div class="reset-container">
      <mat-card>
        <mat-card-header><mat-card-title>{{ 'reset_password.title' | translate }}</mat-card-title></mat-card-header>
        <mat-card-content>
          @if (success) {
            <div class="success-msg"><mat-icon>check_circle</mat-icon> {{ 'reset_password.success' | translate }}</div>
            <a mat-raised-button color="primary" routerLink="/login" class="full-width">{{ 'login.submit' | translate }}</a>
          } @else {
            <form [formGroup]="form" (ngSubmit)="submit()">
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>{{ 'reset_password.password' | translate }}</mat-label>
                <input matInput formControlName="password" type="password" />
              </mat-form-field>
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>{{ 'reset_password.confirm' | translate }}</mat-label>
                <input matInput formControlName="confirm" type="password" />
              </mat-form-field>
              @if (mismatch) { <p class="error">{{ 'reset_password.mismatch' | translate }}</p> }
              @if (error) { <p class="error">{{ error }}</p> }
              <button mat-raised-button color="primary" type="submit" [disabled]="form.invalid || loading" class="full-width">
                {{ (loading ? 'reset_password.saving' : 'reset_password.submit') | translate }}
              </button>
            </form>
          }
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .reset-container { max-width: 420px; margin: 80px auto; padding: 24px; }
    .full-width { width: 100%; }
    .success-msg { color: #2e7d32; display: flex; align-items: center; gap: 8px; margin: 24px 0; }
    .error { color: #c62828; }
  `]
})
export class ResetPasswordComponent {
  private fb = inject(FormBuilder);
  private http = inject(HttpClient);
  private route = inject(ActivatedRoute);
  private translate = inject(TranslateService);

  form = this.fb.group({
    password: ['', [Validators.required, Validators.minLength(6)]],
    confirm: ['', Validators.required]
  });
  loading = false;
  success = false;
  error = '';

  get mismatch(): boolean {
    return this.form.value.password !== this.form.value.confirm && !!this.form.value.confirm;
  }

  submit(): void {
    if (this.mismatch) return;
    this.loading = true;
    this.error = '';
    const token = this.route.snapshot.queryParamMap.get('token');
    this.http.post('/api/auth/reset-password', { token, password: this.form.value.password }).subscribe({
      next: () => { this.loading = false; this.success = true; },
      error: () => { this.loading = false; this.error = this.translate.instant('reset_password.error'); }
    });
  }
}
