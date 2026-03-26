import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { FuelPriceService, FuelPrice } from '../shared/services/fuel-price.service';
import { AuthService } from '../shared/services/auth.service';

interface CountryGroup {
  country: string;
  countryName: string;
  flag: string;
  prices: FuelPrice[];
}

const COUNTRY_NAMES: Record<string, { name: string; flag: string }> = {
  'EE': { name: 'Eesti', flag: '🇪🇪' },
  'LV': { name: 'Läti', flag: '🇱🇻' },
  'LT': { name: 'Leedu', flag: '🇱🇹' },
  'FI': { name: 'Soome', flag: '🇫🇮' },
  'PL': { name: 'Poola', flag: '🇵🇱' },
  'DE': { name: 'Saksamaa', flag: '🇩🇪' },
  'SE': { name: 'Rootsi', flag: '🇸🇪' },
  'CZ': { name: 'Tšehhi', flag: '🇨🇿' },
  'AT': { name: 'Austria', flag: '🇦🇹' },
  'HU': { name: 'Ungari', flag: '🇭🇺' },
  'FR': { name: 'Prantsusmaa', flag: '🇫🇷' },
  'NL': { name: 'Holland', flag: '🇳🇱' },
  'BE': { name: 'Belgia', flag: '🇧🇪' },
  'IT': { name: 'Itaalia', flag: '🇮🇹' },
  'ES': { name: 'Hispaania', flag: '🇪🇸' },
  'CH': { name: 'Šveits', flag: '🇨🇭' },
  'DK': { name: 'Taani', flag: '🇩🇰' },
  'NO': { name: 'Norra', flag: '🇳🇴' },
  'SK': { name: 'Slovakkia', flag: '🇸🇰' },
  'RO': { name: 'Rumeenia', flag: '🇷🇴' },
  'BG': { name: 'Bulgaaria', flag: '🇧🇬' },
  'HR': { name: 'Horvaatia', flag: '🇭🇷' },
  'SI': { name: 'Sloveenia', flag: '🇸🇮' },
};

@Component({
  selector: 'app-fuel-prices',
  standalone: true,
  imports: [
    CommonModule, MatCardModule, MatTableModule, MatIconModule, MatChipsModule,
    MatButtonModule, MatSnackBarModule, TranslateModule
  ],
  template: `
    <div class="fuel-prices-container">
      <div class="header">
        <div class="header-row">
          <div>
            <h1>{{ 'fuel_prices.title' | translate }}</h1>
            <p class="subtitle">{{ 'fuel_prices.subtitle' | translate }}</p>
          </div>
          @if (isLoggedIn) {
            <button mat-raised-button color="primary" (click)="refreshPrices()" [disabled]="refreshing">
              <mat-icon>{{ refreshing ? 'hourglass_empty' : 'refresh' }}</mat-icon>
              {{ (refreshing ? 'fuel_prices.refreshing' : 'fuel_prices.refresh') | translate }}
            </button>
          }
        </div>
      </div>

      @if (loading) {
        <div class="loading">
          <mat-icon>hourglass_empty</mat-icon>
          <span>{{ 'fuel_prices.loading' | translate }}</span>
        </div>
      }

      @if (!loading && countryGroups.length === 0) {
        <mat-card class="empty-card">
          <mat-icon>info</mat-icon>
          <p>{{ 'fuel_prices.no_data' | translate }}</p>
        </mat-card>
      }

      <div class="grid">
        @for (group of countryGroups; track group.country) {
          <mat-card class="country-card">
            <div class="country-header">
              <span class="flag">{{ group.flag }}</span>
              <span class="country-name">{{ group.countryName }}</span>
              <span class="country-code">{{ group.country }}</span>
            </div>
            <div class="prices">
              @for (price of group.prices; track price.id) {
                <div class="price-row">
                  <div class="fuel-type">
                    <mat-icon>{{ getFuelIcon(price.fuelType) }}</mat-icon>
                    <span>{{ price.fuelType }}</span>
                  </div>
                  <div class="price-value">
                    <span class="amount">{{ price.pricePerLiter | number:'1.3-3' }}</span>
                    <span class="unit">€/L</span>
                  </div>
                </div>
              }
            </div>
            <div class="updated">
              {{ 'fuel_prices.valid_from' | translate }}: {{ group.prices[0]?.validFrom }}
            </div>
          </mat-card>
        }
      </div>
    </div>
  `,
  styleUrl: './fuel-prices.component.scss'
})
export class FuelPricesComponent implements OnInit {
  private fuelPriceService = inject(FuelPriceService);
  private authService = inject(AuthService);
  private snackBar = inject(MatSnackBar);
  private translate = inject(TranslateService);

  countryGroups: CountryGroup[] = [];
  loading = true;
  refreshing = false;
  isLoggedIn = false;

  ngOnInit(): void {
    this.isLoggedIn = this.authService.isLoggedIn();
    this.loadPrices();
  }

  private loadPrices(): void {
    this.fuelPriceService.getAll().subscribe({
      next: prices => {
        this.countryGroups = this.groupByCountry(prices);
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  refreshPrices(): void {
    this.refreshing = true;
    this.fuelPriceService.refresh().subscribe({
      next: res => {
        this.refreshing = false;
        this.snackBar.open(
          this.translate.instant('fuel_prices.refresh_success', { count: res.updatedCount }),
          'OK', { duration: 4000 }
        );
        this.loadPrices();
      },
      error: () => {
        this.refreshing = false;
        this.snackBar.open(
          this.translate.instant('fuel_prices.refresh_error'),
          'OK', { duration: 4000 }
        );
      }
    });
  }

  getFuelIcon(fuelType: string): string {
    switch (fuelType) {
      case 'DIESEL': return 'local_gas_station';
      case 'PETROL': return 'local_gas_station';
      case 'LNG': return 'propane';
      case 'CNG': return 'propane';
      default: return 'local_gas_station';
    }
  }

  private groupByCountry(prices: FuelPrice[]): CountryGroup[] {
    const map = new Map<string, FuelPrice[]>();
    for (const p of prices) {
      const list = map.get(p.countryCode) || [];
      list.push(p);
      map.set(p.countryCode, list);
    }
    return Array.from(map.entries())
      .map(([code, prices]) => ({
        country: code,
        countryName: COUNTRY_NAMES[code]?.name || code,
        flag: COUNTRY_NAMES[code]?.flag || '🏳️',
        prices: prices.sort((a, b) => a.fuelType.localeCompare(b.fuelType))
      }))
      .sort((a, b) => a.countryName.localeCompare(b.countryName));
  }
}
