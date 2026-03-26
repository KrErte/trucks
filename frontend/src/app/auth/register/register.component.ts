import { Component, inject } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { RouterLink, Router } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { HttpClient } from '@angular/common/http';
import { debounceTime, distinctUntilChanged, switchMap, of, catchError } from 'rxjs';
import { AuthService } from '../../shared/services/auth.service';
interface CompanyResult {
  company_id: number;
  reg_code: string;
  name: string;
  status: string;
  legal_address: string;
}

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, MatCardModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule, MatAutocompleteModule, RouterLink, TranslateModule],
  template: `
    <div class="register-container">
      <mat-card>
        <mat-card-header>
          <mat-card-title>{{ 'register.title' | translate }}</mat-card-title>
          <mat-card-subtitle>{{ 'register.subtitle' | translate }}</mat-card-subtitle>
        </mat-card-header>
        <mat-card-content>
          <form [formGroup]="form" (ngSubmit)="onSubmit()">
            <div class="row">
              <mat-form-field appearance="outline">
                <mat-label>{{ 'register.first_name' | translate }}</mat-label>
                <input matInput formControlName="firstName" />
              </mat-form-field>
              <mat-form-field appearance="outline">
                <mat-label>{{ 'register.last_name' | translate }}</mat-label>
                <input matInput formControlName="lastName" />
              </mat-form-field>
            </div>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>{{ 'register.email' | translate }}</mat-label>
              <input matInput formControlName="email" type="email" />
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>{{ 'register.password' | translate }}</mat-label>
              <input matInput formControlName="password" [type]="hidePassword ? 'password' : 'text'" />
              <button mat-icon-button matSuffix type="button" (click)="hidePassword = !hidePassword">
                <mat-icon>{{ hidePassword ? 'visibility_off' : 'visibility' }}</mat-icon>
              </button>
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>{{ 'register.company_name' | translate }}</mat-label>
              <input matInput formControlName="companyName" [matAutocomplete]="companyAuto" />
              <mat-icon matSuffix>search</mat-icon>
              <mat-autocomplete #companyAuto="matAutocomplete" (optionSelected)="onCompanySelected($event)">
                @for (company of companySuggestions; track company.reg_code) {
                  <mat-option [value]="company.name">
                    <div class="company-option">
                      <strong>{{ company.name }}</strong>
                      <span class="company-code">{{ company.reg_code }}</span>
                    </div>
                  </mat-option>
                }
              </mat-autocomplete>
            </mat-form-field>

            <div class="row">
              <mat-form-field appearance="outline">
                <mat-label>{{ 'register.reg_code' | translate }}</mat-label>
                <input matInput formControlName="regCode" />
              </mat-form-field>
              <mat-form-field appearance="outline">
                <mat-label>{{ 'register.vat_number' | translate }}</mat-label>
                <input matInput formControlName="vatNumber" />
              </mat-form-field>
            </div>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>{{ 'register.country' | translate }}</mat-label>
              <input matInput formControlName="country" />
            </mat-form-field>

            @if (errorMessage) {
              <p class="error-message">{{ errorMessage }}</p>
            }

            @if (registered) {
              <div class="verify-notice">
                <mat-icon>mark_email_read</mat-icon>
                <p>{{ 'register.check_email' | translate }}</p>
              </div>
            }

            <button mat-raised-button color="primary" type="submit" class="full-width" [disabled]="form.invalid || loading || registered">
              {{ loading ? ('register.registering' | translate) : ('register.submit' | translate) }}
            </button>
          </form>
        </mat-card-content>
        <mat-card-actions align="end">
          <a mat-button routerLink="/login">{{ 'register.has_account' | translate }}</a>
        </mat-card-actions>
      </mat-card>
    </div>
  `,
  styleUrl: './register.component.scss'
})
export class RegisterComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
  private translate = inject(TranslateService);
  private http = inject(HttpClient);

  hidePassword = true;
  loading = false;
  registered = false;
  errorMessage = '';
  companySuggestions: CompanyResult[] = [];

  form = this.fb.group({
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    companyName: ['', Validators.required],
    regCode: [''],
    vatNumber: [''],
    country: ['']
  });

  constructor() {
    this.form.get('companyName')!.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap(val => {
        if (!val || val.length < 2) return of([]);
        return this.http.get<any>(`/api/company-search?q=${encodeURIComponent(val)}`).pipe(
          switchMap(res => of(res?.data || res || [])),
          catchError(() => of([]))
        );
      })
    ).subscribe(results => {
      this.companySuggestions = results || [];
    });
  }

  onCompanySelected(event: any): void {
    const company = this.companySuggestions.find(c => c.name === event.option.value);
    if (company) {
      this.form.patchValue({
        regCode: company.reg_code,
        vatNumber: company.reg_code ? `EE${company.reg_code}` : '',
        country: 'EE'
      });
    }
  }

  onSubmit(): void {
    if (this.form.invalid) return;
    this.loading = true;
    this.errorMessage = '';
    this.authService.register(this.form.value as any).subscribe({
      next: () => this.router.navigate(['/calculator']),
      error: (err) => {
        this.loading = false;
        this.errorMessage = err.error?.message || this.translate.instant('register.error');
      }
    });
  }
}
