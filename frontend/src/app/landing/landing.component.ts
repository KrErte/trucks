import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [RouterLink, MatButtonModule, MatIconModule, MatMenuModule, TranslateModule],
  template: `
    <div class="landing">
      <!-- Header -->
      <header class="header">
        <div class="header-inner">
          <span class="logo">{{ 'brand' | translate }}</span>
          <nav>
            <button mat-icon-button [matMenuTriggerFor]="langMenu" class="lang-btn">
              <mat-icon>language</mat-icon>
            </button>
            <mat-menu #langMenu="matMenu">
              <button mat-menu-item (click)="switchLang('et')">🇪🇪 Eesti</button>
              <button mat-menu-item (click)="switchLang('en')">🇬🇧 English</button>
            </mat-menu>
            <a mat-button routerLink="/login" class="nav-btn">{{ 'nav.login' | translate }}</a>
            <a mat-flat-button routerLink="/register" class="nav-btn-primary">{{ 'nav.register' | translate }}</a>
          </nav>
        </div>
      </header>

      <!-- Hero -->
      <section class="hero">
        <div class="hero-inner">
          <h1>{{ 'landing.hero_title' | translate }}</h1>
          <p class="subtitle">{{ 'landing.hero_subtitle' | translate }}</p>
          <div class="trial-badge">{{ 'landing.trial_badge' | translate }}</div>
          <div class="hero-actions">
            <a mat-flat-button routerLink="/register" class="cta-btn">{{ 'landing.cta' | translate }}</a>
            <a mat-button routerLink="/login" class="cta-secondary">{{ 'landing.cta_login' | translate }}</a>
          </div>
        </div>
      </section>

      <!-- Features -->
      <section class="features">
        <div class="features-inner">
          <div class="feature">
            <div class="feature-icon"><mat-icon>calculate</mat-icon></div>
            <h3>{{ 'landing.feature_calc_title' | translate }}</h3>
            <p>{{ 'landing.feature_calc_desc' | translate }}</p>
          </div>
          <div class="feature">
            <div class="feature-icon"><mat-icon>route</mat-icon></div>
            <h3>{{ 'landing.feature_route_title' | translate }}</h3>
            <p>{{ 'landing.feature_route_desc' | translate }}</p>
          </div>
          <div class="feature">
            <div class="feature-icon"><mat-icon>local_gas_station</mat-icon></div>
            <h3>{{ 'landing.feature_fuel_title' | translate }}</h3>
            <p>{{ 'landing.feature_fuel_desc' | translate }}</p>
          </div>
<div class="feature">
            <div class="feature-icon"><mat-icon>trending_up</mat-icon></div>
            <h3>{{ 'landing.feature_profit_title' | translate }}</h3>
            <p>{{ 'landing.feature_profit_desc' | translate }}</p>
          </div>
          <div class="feature">
            <div class="feature-icon"><mat-icon>history</mat-icon></div>
            <h3>{{ 'landing.feature_history_title' | translate }}</h3>
            <p>{{ 'landing.feature_history_desc' | translate }}</p>
          </div>
        </div>
      </section>

      <!-- Pricing -->
      <section class="pricing">
        <div class="pricing-inner">
          <h2>{{ 'landing.pricing' | translate }}</h2>
          <div class="plans">
            <div class="plan">
              <h3>Algaja</h3>
              <div class="price">&euro;49<span>{{ 'landing.per_month' | translate }}</span></div>
              <ul>
                <li>{{ 'landing.starter_vehicles' | translate }}</li>
                <li>{{ 'landing.unlimited_calc' | translate }}</li>
                <li>{{ 'landing.order_history' | translate }}</li>
                <li>{{ 'landing.email_support' | translate }}</li>
              </ul>
              <a mat-flat-button routerLink="/register" class="plan-btn">{{ 'landing.start' | translate }}</a>
            </div>
            <div class="plan plan-featured">
              <h3>Edasijõudnud</h3>
              <div class="price">&euro;129<span>{{ 'landing.per_month' | translate }}</span></div>
              <ul>
                <li>{{ 'landing.growth_vehicles' | translate }}</li>
                <li>{{ 'landing.unlimited_calc' | translate }}</li>
                <li>{{ 'landing.stats_reports' | translate }}</li>
                <li>{{ 'landing.priority_support' | translate }}</li>
              </ul>
              <a mat-flat-button routerLink="/register" class="plan-btn">{{ 'landing.start' | translate }}</a>
            </div>
            <div class="plan">
              <h3>Suurfirma</h3>
              <div class="price">&euro;199<span>{{ 'landing.per_month' | translate }}</span></div>
              <ul>
                <li>{{ 'landing.unlimited_vehicles' | translate }}</li>
                <li>{{ 'landing.api_access' | translate }}</li>
                <li>{{ 'landing.custom_integrations' | translate }}</li>
                <li>{{ 'landing.personal_support' | translate }}</li>
              </ul>
              <a mat-flat-button routerLink="/register" class="plan-btn">{{ 'landing.contact' | translate }}</a>
            </div>
          </div>
        </div>
      </section>

      <!-- Footer -->
      <footer class="footer">
        <div class="footer-inner">
          <span class="footer-brand">{{ 'brand' | translate }}</span>
          <span class="footer-copy">{{ 'landing.footer_copy' | translate }}</span>
        </div>
      </footer>
    </div>
  `,
  styleUrl: './landing.component.scss'
})
export class LandingComponent {
  private translate = inject(TranslateService);

  switchLang(lang: string): void {
    this.translate.use(lang);
    localStorage.setItem('lang', lang);
  }
}
