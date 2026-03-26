import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { CompanyService } from '../shared/services/company.service';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatCardModule, MatFormFieldModule,
    MatInputModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule,
    TranslateModule
  ],
  template: `
    <div class="settings-container">
      <h1>{{ 'settings.title' | translate }}</h1>

      <mat-card>
        <mat-card-header>
          <mat-card-title>{{ 'settings.company_data' | translate }}</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          @if (loadingData) {
            <div style="display: flex; justify-content: center; padding: 32px;">
              <mat-spinner diameter="40"></mat-spinner>
            </div>
          } @else {
            <form [formGroup]="form" (ngSubmit)="onSave()">
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>{{ 'settings.company_name' | translate }}</mat-label>
                <input matInput formControlName="companyName" />
              </mat-form-field>

              <mat-form-field appearance="outline" class="full-width">
                <mat-label>{{ 'settings.vat_number' | translate }}</mat-label>
                <input matInput formControlName="vatNumber" />
              </mat-form-field>

              <mat-form-field appearance="outline" class="full-width">
                <mat-label>{{ 'settings.country' | translate }}</mat-label>
                <input matInput formControlName="country" />
              </mat-form-field>

              <mat-form-field appearance="outline" class="full-width">
                <mat-label>{{ 'settings.driver_daily_rate' | translate }}</mat-label>
                <input matInput formControlName="defaultDriverDailyRate" type="number" />
              </mat-form-field>

              <mat-form-field appearance="outline" class="full-width">
                <mat-label>{{ 'settings.min_margin' | translate }}</mat-label>
                <input matInput formControlName="minMarginPct" type="number" />
                <span matSuffix>%</span>
                <mat-hint>{{ 'settings.min_margin_hint' | translate }}</mat-hint>
              </mat-form-field>

              <div class="actions">
                <button mat-raised-button color="primary" type="submit" [disabled]="saving || form.pristine">
                  @if (saving) {
                    <mat-spinner diameter="20"></mat-spinner>
                  } @else {
                    <mat-icon>save</mat-icon> {{ 'settings.save' | translate }}
                  }
                </button>
                @if (successMessage) {
                  <span class="success-message">{{ successMessage }}</span>
                }
                @if (errorMessage) {
                  <span class="error-message">{{ errorMessage }}</span>
                }
              </div>
            </form>
          }
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styleUrl: './settings.component.scss'
})
export class SettingsComponent implements OnInit {
  private fb = inject(FormBuilder);
  private companyService = inject(CompanyService);
  private translate = inject(TranslateService);

  loadingData = true;
  saving = false;
  successMessage = '';
  errorMessage = '';

  form = this.fb.group({
    companyName: ['', Validators.required],
    vatNumber: [''],
    country: [''],
    defaultDriverDailyRate: [null as number | null],
    minMarginPct: [10 as number | null]
  });

  ngOnInit(): void {
    this.companyService.getSettings().subscribe({
      next: (settings) => {
        this.form.patchValue(settings);
        const savedMargin = localStorage.getItem('minMarginPct');
        if (savedMargin) this.form.patchValue({ minMarginPct: parseFloat(savedMargin) });
        this.form.markAsPristine();
        this.loadingData = false;
      },
      error: () => {
        this.errorMessage = this.translate.instant('settings.load_error');
        this.loadingData = false;
      }
    });
  }

  onSave(): void {
    if (this.form.invalid) return;
    this.saving = true;
    this.successMessage = '';
    this.errorMessage = '';

    const minMargin = this.form.value.minMarginPct;
    if (minMargin !== null && minMargin !== undefined) {
      localStorage.setItem('minMarginPct', String(minMargin));
    }
    const { minMarginPct, ...settings } = this.form.value;
    this.companyService.updateSettings(settings as any).subscribe({
      next: (settings) => {
        this.form.patchValue(settings);
        this.form.markAsPristine();
        this.saving = false;
        this.successMessage = this.translate.instant('settings.saved');
        setTimeout(() => this.successMessage = '', 3000);
      },
      error: () => {
        this.saving = false;
        this.errorMessage = this.translate.instant('settings.save_error');
      }
    });
  }
}
