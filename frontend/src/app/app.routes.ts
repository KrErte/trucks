import { Routes } from '@angular/router';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { authGuard } from './shared/guards/auth.guard';
import { roleGuard } from './shared/guards/role.guard';
import { AuthService } from './shared/services/auth.service';

const landingGuard = () => {
  const authService = inject(AuthService);
  const router = inject(Router);
  if (authService.isLoggedIn()) {
    return router.createUrlTree(['/dashboard']);
  }
  return true;
};

export const routes: Routes = [
  { path: '', loadComponent: () => import('./landing/landing.component').then(m => m.LandingComponent), canActivate: [landingGuard] },
  { path: 'login', loadComponent: () => import('./auth/login/login.component').then(m => m.LoginComponent) },
  { path: 'register', loadComponent: () => import('./auth/register/register.component').then(m => m.RegisterComponent) },
  { path: 'verify', loadComponent: () => import('./auth/verify/verify.component').then(m => m.VerifyComponent) },
  { path: 'forgot-password', loadComponent: () => import('./auth/forgot-password/forgot-password.component').then(m => m.ForgotPasswordComponent) },
  { path: 'reset-password', loadComponent: () => import('./auth/reset-password/reset-password.component').then(m => m.ResetPasswordComponent) },
  { path: 'dashboard', loadComponent: () => import('./dashboard/dashboard.component').then(m => m.DashboardComponent), canActivate: [authGuard] },
  { path: 'calculator', loadComponent: () => import('./calculator/calculator.component').then(m => m.CalculatorComponent), canActivate: [authGuard] },
  { path: 'vehicles', loadComponent: () => import('./vehicles/vehicles.component').then(m => m.VehiclesComponent), canActivate: [authGuard] },
  { path: 'history', loadComponent: () => import('./history/history.component').then(m => m.HistoryComponent), canActivate: [authGuard] },
  { path: 'fuel-prices', loadComponent: () => import('./fuel-prices/fuel-prices.component').then(m => m.FuelPricesComponent), canActivate: [authGuard] },
  { path: 'profile', loadComponent: () => import('./profile/profile.component').then(m => m.ProfileComponent), canActivate: [authGuard] },
  { path: 'settings', loadComponent: () => import('./settings/settings.component').then(m => m.SettingsComponent), canActivate: [authGuard] },
  { path: 'users', loadComponent: () => import('./user-management/user-management.component').then(m => m.UserManagementComponent), canActivate: [authGuard, roleGuard('ADMIN')] },
  { path: 'accept-invite', loadComponent: () => import('./accept-invite/accept-invite.component').then(m => m.AcceptInviteComponent) },
  { path: 'analytics', loadComponent: () => import('./analytics/analytics.component').then(m => m.AnalyticsComponent), canActivate: [authGuard] },
  { path: 'webhooks', loadComponent: () => import('./webhooks/webhooks.component').then(m => m.WebhooksComponent), canActivate: [authGuard, roleGuard('ADMIN')] },
  { path: 'invoices', loadComponent: () => import('./invoices/invoices.component').then(m => m.InvoicesComponent), canActivate: [authGuard] },
  { path: '**', redirectTo: '' }
];
