import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-verify',
  standalone: true,
  imports: [CommonModule, RouterLink, MatCardModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule, TranslateModule],
  template: `
    <div class="verify-container">
      <mat-card>
        <mat-card-content>
          @if (loading) {
            <div class="center"><mat-spinner diameter="40"></mat-spinner><p>{{ 'verify.checking' | translate }}</p></div>
          } @else if (success) {
            <div class="center success">
              <mat-icon>check_circle</mat-icon>
              <p>{{ 'verify.success' | translate }}</p>
              <a mat-raised-button color="primary" routerLink="/login">{{ 'login.submit' | translate }}</a>
            </div>
          } @else {
            <div class="center error">
              <mat-icon>error</mat-icon>
              <p>{{ 'verify.error' | translate }}</p>
              <a mat-raised-button routerLink="/login">{{ 'login.submit' | translate }}</a>
            </div>
          }
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .verify-container { max-width: 480px; margin: 80px auto; padding: 24px; }
    .center { text-align: center; padding: 32px; }
    .center mat-icon { font-size: 48px; width: 48px; height: 48px; margin-bottom: 16px; }
    .success mat-icon { color: #2e7d32; }
    .error mat-icon { color: #c62828; }
    .center p { font-size: 16px; margin-bottom: 24px; }
  `]
})
export class VerifyComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private http = inject(HttpClient);
  loading = true;
  success = false;

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');
    if (!token) { this.loading = false; return; }
    this.http.get(`/api/auth/verify?token=${token}`).subscribe({
      next: () => { this.loading = false; this.success = true; },
      error: () => { this.loading = false; }
    });
  }
}
