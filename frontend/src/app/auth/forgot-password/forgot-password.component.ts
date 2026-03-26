import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, MatCardModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule, TranslateModule],
  template: `
    <div class="forgot-container">
      <mat-card>
        <mat-card-header><mat-card-title>{{ 'forgot_password.title' | translate }}</mat-card-title></mat-card-header>
        <mat-card-content>
          <p class="desc">{{ 'forgot_password.description' | translate }}</p>
          @if (sent) {
            <div class="success-msg"><mat-icon>check_circle</mat-icon> {{ 'forgot_password.sent' | translate }}</div>
          } @else {
            <form [formGroup]="form" (ngSubmit)="submit()">
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>{{ 'forgot_password.email' | translate }}</mat-label>
                <input matInput formControlName="email" type="email" />
              </mat-form-field>
              @if (error) { <p class="error">{{ error }}</p> }
              <button mat-raised-button color="primary" type="submit" [disabled]="form.invalid || loading" class="full-width">
                {{ (loading ? 'forgot_password.sending' : 'forgot_password.submit') | translate }}
              </button>
            </form>
          }
          <p class="link"><a routerLink="/login">{{ 'login.submit' | translate }}</a></p>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .forgot-container { max-width: 420px; margin: 80px auto; padding: 24px; }
    .desc { color: #666; margin-bottom: 24px; }
    .full-width { width: 100%; }
    .success-msg { color: #2e7d32; display: flex; align-items: center; gap: 8px; margin: 24px 0; }
    .error { color: #c62828; }
    .link { text-align: center; margin-top: 16px; }
    .link a { color: #1a237e; }
  `]
})
export class ForgotPasswordComponent {
  private fb = inject(FormBuilder);
  private http = inject(HttpClient);
  private translate = inject(TranslateService);
  form = this.fb.group({ email: ['', [Validators.required, Validators.email]] });
  loading = false;
  sent = false;
  error = '';

  submit(): void {
    this.loading = true;
    this.error = '';
    this.http.post('/api/auth/forgot-password', { email: this.form.value.email }).subscribe({
      next: () => { this.loading = false; this.sent = true; },
      error: () => { this.loading = false; this.error = this.translate.instant('forgot_password.error'); }
    });
  }
}
