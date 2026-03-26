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
import { ProfileService } from '../shared/services/profile.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatCardModule, MatFormFieldModule,
    MatInputModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule, TranslateModule
  ],
  template: `
    <div class="profile-container">
      <h1>{{ 'profile.title' | translate }}</h1>

      <mat-card class="profile-card">
        <mat-card-header>
          <mat-card-title>{{ 'profile.personal_info' | translate }}</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <form [formGroup]="profileForm" (ngSubmit)="saveProfile()">
            <div class="form-grid">
              <mat-form-field appearance="outline">
                <mat-label>{{ 'profile.first_name' | translate }}</mat-label>
                <input matInput formControlName="firstName" />
              </mat-form-field>

              <mat-form-field appearance="outline">
                <mat-label>{{ 'profile.last_name' | translate }}</mat-label>
                <input matInput formControlName="lastName" />
              </mat-form-field>

              <mat-form-field appearance="outline">
                <mat-label>{{ 'profile.email' | translate }}</mat-label>
                <input matInput formControlName="email" readonly />
              </mat-form-field>
            </div>

            <div class="actions">
              <button mat-raised-button color="primary" type="submit" [disabled]="profileForm.invalid || profileLoading">
                @if (profileLoading) {
                  <mat-spinner diameter="20"></mat-spinner>
                } @else {
                  {{ 'profile.save' | translate }}
                }
              </button>
            </div>
            @if (profileSuccess) {
              <p class="success-message">{{ 'profile.saved' | translate }}</p>
            }
            @if (profileError) {
              <p class="error-message">{{ profileError }}</p>
            }
          </form>
        </mat-card-content>
      </mat-card>

      <mat-card class="profile-card">
        <mat-card-header>
          <mat-card-title>{{ 'profile.change_password' | translate }}</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <form [formGroup]="passwordForm" (ngSubmit)="changePassword()">
            <div class="form-grid">
              <mat-form-field appearance="outline">
                <mat-label>{{ 'profile.current_password' | translate }}</mat-label>
                <input matInput formControlName="currentPassword" type="password" />
              </mat-form-field>

              <mat-form-field appearance="outline">
                <mat-label>{{ 'profile.new_password' | translate }}</mat-label>
                <input matInput formControlName="newPassword" type="password" />
              </mat-form-field>

              <mat-form-field appearance="outline">
                <mat-label>{{ 'profile.confirm_password' | translate }}</mat-label>
                <input matInput formControlName="confirmPassword" type="password" />
              </mat-form-field>
            </div>

            @if (passwordForm.hasError('mismatch')) {
              <p class="error-message">{{ 'profile.password_mismatch' | translate }}</p>
            }

            <div class="actions">
              <button mat-raised-button color="primary" type="submit" [disabled]="passwordForm.invalid || passwordLoading">
                @if (passwordLoading) {
                  <mat-spinner diameter="20"></mat-spinner>
                } @else {
                  {{ 'profile.change_password' | translate }}
                }
              </button>
            </div>
            @if (passwordSuccess) {
              <p class="success-message">{{ 'profile.password_changed' | translate }}</p>
            }
            @if (passwordError) {
              <p class="error-message">{{ passwordError }}</p>
            }
          </form>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styleUrl: './profile.component.scss'
})
export class ProfileComponent implements OnInit {
  private fb = inject(FormBuilder);
  private profileService = inject(ProfileService);
  private translate = inject(TranslateService);

  profileForm = this.fb.group({
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    email: [{ value: '', disabled: true }]
  });

  passwordForm = this.fb.group({
    currentPassword: ['', Validators.required],
    newPassword: ['', [Validators.required, Validators.minLength(6)]],
    confirmPassword: ['', Validators.required]
  }, { validators: this.passwordMatchValidator });

  profileLoading = false;
  profileSuccess = false;
  profileError = '';
  passwordLoading = false;
  passwordSuccess = false;
  passwordError = '';

  ngOnInit(): void {
    this.profileService.getProfile().subscribe({
      next: (p) => {
        this.profileForm.patchValue({
          firstName: p.firstName,
          lastName: p.lastName,
          email: p.email
        });
      },
      error: () => this.profileError = this.translate.instant('profile.load_error')
    });
  }

  saveProfile(): void {
    if (this.profileForm.invalid) return;
    this.profileLoading = true;
    this.profileSuccess = false;
    this.profileError = '';
    this.profileService.updateProfile({
      firstName: this.profileForm.value.firstName!,
      lastName: this.profileForm.value.lastName!
    }).subscribe({
      next: () => {
        this.profileLoading = false;
        this.profileSuccess = true;
      },
      error: (err) => {
        this.profileLoading = false;
        this.profileError = err.error?.message || this.translate.instant('profile.save_error');
      }
    });
  }

  changePassword(): void {
    if (this.passwordForm.invalid) return;
    this.passwordLoading = true;
    this.passwordSuccess = false;
    this.passwordError = '';
    this.profileService.changePassword({
      currentPassword: this.passwordForm.value.currentPassword!,
      newPassword: this.passwordForm.value.newPassword!
    }).subscribe({
      next: () => {
        this.passwordLoading = false;
        this.passwordSuccess = true;
        this.passwordForm.reset();
      },
      error: (err) => {
        this.passwordLoading = false;
        this.passwordError = err.error?.message || this.translate.instant('profile.password_error');
      }
    });
  }

  private passwordMatchValidator(group: any) {
    const newPassword = group.get('newPassword')?.value;
    const confirmPassword = group.get('confirmPassword')?.value;
    return newPassword === confirmPassword ? null : { mismatch: true };
  }
}
