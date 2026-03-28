import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, RouterLink, RouterLinkActive, Router } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatBadgeModule } from '@angular/material/badge';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AuthService } from './shared/services/auth.service';
import { PwaInstallService } from './shared/services/pwa-install.service';
import { NotificationService, AppNotification } from './shared/services/notification.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule, RouterOutlet, RouterLink, RouterLinkActive,
    MatToolbarModule, MatSidenavModule, MatListModule, MatIconModule, MatButtonModule,
    MatMenuModule, MatBadgeModule, TranslateModule
  ],
  template: `
    @if (authService.isLoggedIn()) {
      <mat-toolbar color="primary" class="toolbar">
        <button mat-icon-button (click)="sidenav.toggle()" class="menu-button">
          <mat-icon>menu</mat-icon>
        </button>
        <span class="brand">{{ 'brand' | translate }}</span>

        <nav class="nav-links">
          <a mat-button routerLink="/dashboard" routerLinkActive="active-link">
            <mat-icon>dashboard</mat-icon> {{ 'nav.dashboard' | translate }}
          </a>
          <a mat-button routerLink="/calculator" routerLinkActive="active-link">
            <mat-icon>calculate</mat-icon> {{ 'nav.calculator' | translate }}
          </a>
          <a mat-button routerLink="/vehicles" routerLinkActive="active-link">
            <mat-icon>local_shipping</mat-icon> {{ 'nav.vehicles' | translate }}
          </a>
          <a mat-button routerLink="/history" routerLinkActive="active-link">
            <mat-icon>history</mat-icon> {{ 'nav.history' | translate }}
          </a>
          <a mat-button routerLink="/fuel-prices" routerLinkActive="active-link">
            <mat-icon>local_gas_station</mat-icon> {{ 'nav.fuel_prices' | translate }}
          </a>
          <a mat-button routerLink="/analytics" routerLinkActive="active-link">
            <mat-icon>analytics</mat-icon> {{ 'nav.analytics' | translate }}
          </a>
          <a mat-button routerLink="/drivers" routerLinkActive="active-link">
            <mat-icon>badge</mat-icon> {{ 'nav.drivers' | translate }}
          </a>
          <a mat-button routerLink="/customers" routerLinkActive="active-link">
            <mat-icon>people</mat-icon> {{ 'nav.customers' | translate }}
          </a>
          <a mat-button routerLink="/maintenance" routerLinkActive="active-link">
            <mat-icon>build</mat-icon> {{ 'nav.maintenance' | translate }}
          </a>
          <a mat-button routerLink="/documents" routerLinkActive="active-link">
            <mat-icon>folder</mat-icon> {{ 'nav.documents' | translate }}
          </a>
          <a mat-button routerLink="/invoices" routerLinkActive="active-link">
            <mat-icon>receipt_long</mat-icon> {{ 'nav.invoices' | translate }}
          </a>
          <a mat-button routerLink="/settings" routerLinkActive="active-link">
            <mat-icon>settings</mat-icon> {{ 'nav.settings' | translate }}
          </a>
        </nav>

        <span class="spacer"></span>

        <button mat-icon-button [matMenuTriggerFor]="notifMenu" class="notif-btn">
          <mat-icon [matBadge]="unreadCount > 0 ? unreadCount : null" matBadgeColor="warn" matBadgeSize="small">notifications</mat-icon>
        </button>
        <mat-menu #notifMenu="matMenu" class="notif-menu">
          @if (notifications.length === 0) {
            <div class="notif-empty">No notifications</div>
          } @else {
            @for (n of notifications; track n.id) {
              <button mat-menu-item (click)="markNotifRead(n.id)">
                <mat-icon>{{ n.read ? 'notifications_none' : 'notifications_active' }}</mat-icon>
                <span>{{ n.title }}</span>
              </button>
            }
            <button mat-menu-item (click)="markAllNotifsRead()">
              <mat-icon>done_all</mat-icon> Mark all read
            </button>
          }
        </mat-menu>

        <button mat-icon-button [matMenuTriggerFor]="langMenu">
          <mat-icon>language</mat-icon>
        </button>
        <mat-menu #langMenu="matMenu">
          <button mat-menu-item (click)="switchLang('et')">
            <span>🇪🇪 Eesti</span>
          </button>
          <button mat-menu-item (click)="switchLang('en')">
            <span>🇬🇧 English</span>
          </button>
        </mat-menu>

        <button mat-icon-button [matMenuTriggerFor]="userMenu">
          <mat-icon>account_circle</mat-icon>
        </button>
        <mat-menu #userMenu="matMenu">
          <a mat-menu-item routerLink="/profile">
            <mat-icon>person</mat-icon> {{ 'nav.profile' | translate }}
          </a>
          <button mat-menu-item (click)="logout()">
            <mat-icon>logout</mat-icon> {{ 'nav.logout' | translate }}
          </button>
        </mat-menu>
      </mat-toolbar>

      <mat-sidenav-container class="sidenav-container">
        <mat-sidenav #sidenav mode="over" class="sidenav">
          <mat-nav-list>
            <a mat-list-item routerLink="/dashboard" (click)="sidenav.close()" routerLinkActive="active-link">
              <mat-icon matListItemIcon>dashboard</mat-icon> {{ 'nav.dashboard' | translate }}
            </a>
            <a mat-list-item routerLink="/calculator" (click)="sidenav.close()" routerLinkActive="active-link">
              <mat-icon matListItemIcon>calculate</mat-icon> {{ 'nav.calculator' | translate }}
            </a>
            <a mat-list-item routerLink="/vehicles" (click)="sidenav.close()" routerLinkActive="active-link">
              <mat-icon matListItemIcon>local_shipping</mat-icon> {{ 'nav.vehicles' | translate }}
            </a>
            <a mat-list-item routerLink="/history" (click)="sidenav.close()" routerLinkActive="active-link">
              <mat-icon matListItemIcon>history</mat-icon> {{ 'nav.history' | translate }}
            </a>
            <a mat-list-item routerLink="/fuel-prices" (click)="sidenav.close()" routerLinkActive="active-link">
              <mat-icon matListItemIcon>local_gas_station</mat-icon> {{ 'nav.fuel_prices' | translate }}
            </a>
            <a mat-list-item routerLink="/analytics" (click)="sidenav.close()" routerLinkActive="active-link">
              <mat-icon matListItemIcon>analytics</mat-icon> {{ 'nav.analytics' | translate }}
            </a>
            <a mat-list-item routerLink="/drivers" (click)="sidenav.close()" routerLinkActive="active-link">
              <mat-icon matListItemIcon>badge</mat-icon> {{ 'nav.drivers' | translate }}
            </a>
            <a mat-list-item routerLink="/customers" (click)="sidenav.close()" routerLinkActive="active-link">
              <mat-icon matListItemIcon>people</mat-icon> {{ 'nav.customers' | translate }}
            </a>
            <a mat-list-item routerLink="/maintenance" (click)="sidenav.close()" routerLinkActive="active-link">
              <mat-icon matListItemIcon>build</mat-icon> {{ 'nav.maintenance' | translate }}
            </a>
            <a mat-list-item routerLink="/documents" (click)="sidenav.close()" routerLinkActive="active-link">
              <mat-icon matListItemIcon>folder</mat-icon> {{ 'nav.documents' | translate }}
            </a>
            <a mat-list-item routerLink="/invoices" (click)="sidenav.close()" routerLinkActive="active-link">
              <mat-icon matListItemIcon>receipt_long</mat-icon> {{ 'nav.invoices' | translate }}
            </a>
            <a mat-list-item routerLink="/settings" (click)="sidenav.close()" routerLinkActive="active-link">
              <mat-icon matListItemIcon>settings</mat-icon> {{ 'nav.settings' | translate }}
            </a>
            <a mat-list-item routerLink="/profile" (click)="sidenav.close()" routerLinkActive="active-link">
              <mat-icon matListItemIcon>person</mat-icon> {{ 'nav.profile' | translate }}
            </a>
          </mat-nav-list>
        </mat-sidenav>

        <mat-sidenav-content>
          <router-outlet />
        </mat-sidenav-content>
      </mat-sidenav-container>
      @if (pwaService.canInstall) {
        <div class="pwa-banner">
          <span>{{ 'pwa.install_prompt' | translate }}</span>
          <button mat-raised-button color="accent" (click)="installPwa()">
            <mat-icon>install_mobile</mat-icon> {{ 'pwa.install' | translate }}
          </button>
        </div>
      }
    } @else {
      <router-outlet />
    }
  `,
  styleUrl: './app.component.scss'
})
export class AppComponent {
  authService = inject(AuthService);
  pwaService = inject(PwaInstallService);
  private notificationService = inject(NotificationService);
  private router = inject(Router);
  private translate = inject(TranslateService);

  notifications: AppNotification[] = [];
  unreadCount = 0;

  constructor() {
    this.translate.addLangs(['et', 'en']);
    this.translate.setDefaultLang('et');
    const saved = localStorage.getItem('lang');
    this.translate.use(saved || 'et');

    if (this.authService.isLoggedIn()) {
      this.loadNotifications();
    }
  }

  loadNotifications(): void {
    this.notificationService.getUnread().subscribe(n => this.notifications = n);
    this.notificationService.unreadCount$.subscribe(c => this.unreadCount = c);
    this.notificationService.refreshUnreadCount();
  }

  markNotifRead(id: string): void {
    this.notificationService.markAsRead(id).subscribe(() => {
      this.notificationService.getUnread().subscribe(n => this.notifications = n);
    });
  }

  markAllNotifsRead(): void {
    this.notificationService.markAllAsRead().subscribe(() => { this.notifications = []; });
  }

  switchLang(lang: string): void {
    this.translate.use(lang);
    localStorage.setItem('lang', lang);
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  installPwa(): void {
    this.pwaService.install();
  }
}
