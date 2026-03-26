import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatExpansionModule } from '@angular/material/expansion';
import { TranslateModule } from '@ngx-translate/core';
import { WebhookService, WebhookEndpoint, WebhookDelivery } from '../shared/services/webhook.service';

@Component({
  selector: 'app-webhooks',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatCardModule, MatTableModule, MatButtonModule,
    MatIconModule, MatFormFieldModule, MatInputModule, MatChipsModule,
    MatDialogModule, MatSlideToggleModule, MatExpansionModule, TranslateModule
  ],
  template: `
    <div class="webhooks-container" style="max-width: 900px; margin: 0 auto; padding: 24px;">
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px;">
        <h1>{{ 'webhooks.title' | translate }}</h1>
        <button mat-raised-button color="primary" (click)="showAddForm = !showAddForm">
          <mat-icon>add</mat-icon> {{ 'webhooks.add' | translate }}
        </button>
      </div>

      @if (showAddForm) {
        <mat-card style="margin-bottom: 24px; padding: 24px;">
          <h3>{{ 'webhooks.new_endpoint' | translate }}</h3>
          <mat-form-field appearance="outline" style="width: 100%;">
            <mat-label>URL</mat-label>
            <input matInput [(ngModel)]="newUrl" placeholder="https://example.com/webhook" />
          </mat-form-field>
          <mat-form-field appearance="outline" style="width: 100%;">
            <mat-label>{{ 'webhooks.events' | translate }}</mat-label>
            <input matInput [(ngModel)]="newEvents" placeholder="calculation.created, invoice.created" />
          </mat-form-field>
          <button mat-raised-button color="primary" (click)="addEndpoint()" [disabled]="!newUrl">
            {{ 'webhooks.save' | translate }}
          </button>
        </mat-card>
      }

      @for (endpoint of endpoints; track endpoint.id) {
        <mat-card style="margin-bottom: 16px; padding: 16px;">
          <div style="display: flex; justify-content: space-between; align-items: center;">
            <div>
              <strong>{{ endpoint.url }}</strong>
              <div style="margin-top: 4px;">
                @for (event of endpoint.events; track event) {
                  <span style="background: #e3f2fd; padding: 2px 8px; border-radius: 12px; margin-right: 4px; font-size: 12px;">{{ event }}</span>
                }
              </div>
              <div style="margin-top: 4px; font-size: 12px; color: #666;">
                Secret: {{ endpoint.secret }}
              </div>
            </div>
            <div style="display: flex; align-items: center; gap: 8px;">
              <mat-slide-toggle [checked]="endpoint.active" (change)="toggleEndpoint(endpoint)"></mat-slide-toggle>
              <button mat-icon-button color="warn" (click)="deleteEndpoint(endpoint.id)">
                <mat-icon>delete</mat-icon>
              </button>
            </div>
          </div>

          <mat-expansion-panel style="margin-top: 12px;">
            <mat-expansion-panel-header>
              <mat-panel-title>{{ 'webhooks.deliveries' | translate }}</mat-panel-title>
            </mat-expansion-panel-header>
            <button mat-button (click)="loadDeliveries(endpoint.id)">
              {{ 'webhooks.load_deliveries' | translate }}
            </button>
            @if (deliveriesMap[endpoint.id]) {
              <table mat-table [dataSource]="deliveriesMap[endpoint.id]" style="width: 100%;">
                <ng-container matColumnDef="eventType">
                  <th mat-header-cell *matHeaderCellDef>{{ 'webhooks.event' | translate }}</th>
                  <td mat-cell *matCellDef="let d">{{ d.eventType }}</td>
                </ng-container>
                <ng-container matColumnDef="responseStatus">
                  <th mat-header-cell *matHeaderCellDef>{{ 'webhooks.status' | translate }}</th>
                  <td mat-cell *matCellDef="let d">
                    <span [style.color]="d.failed ? 'red' : 'green'">{{ d.responseStatus || 'N/A' }}</span>
                  </td>
                </ng-container>
                <ng-container matColumnDef="retryCount">
                  <th mat-header-cell *matHeaderCellDef>{{ 'webhooks.retries' | translate }}</th>
                  <td mat-cell *matCellDef="let d">{{ d.retryCount }}</td>
                </ng-container>
                <ng-container matColumnDef="createdAt">
                  <th mat-header-cell *matHeaderCellDef>{{ 'webhooks.date' | translate }}</th>
                  <td mat-cell *matCellDef="let d">{{ d.createdAt | date:'dd.MM.yyyy HH:mm' }}</td>
                </ng-container>
                <tr mat-header-row *matHeaderRowDef="deliveryColumns"></tr>
                <tr mat-row *matRowDef="let row; columns: deliveryColumns;"></tr>
              </table>
            }
          </mat-expansion-panel>
        </mat-card>
      }

      @if (endpoints.length === 0) {
        <mat-card style="padding: 48px; text-align: center;">
          <mat-icon style="font-size: 48px; width: 48px; height: 48px; color: #ccc;">webhook</mat-icon>
          <p>{{ 'webhooks.empty' | translate }}</p>
        </mat-card>
      }
    </div>
  `
})
export class WebhooksComponent implements OnInit {
  private webhookService = inject(WebhookService);

  endpoints: WebhookEndpoint[] = [];
  showAddForm = false;
  newUrl = '';
  newEvents = '';
  deliveriesMap: Record<string, WebhookDelivery[]> = {};
  deliveryColumns = ['eventType', 'responseStatus', 'retryCount', 'createdAt'];

  ngOnInit(): void {
    this.loadEndpoints();
  }

  loadEndpoints(): void {
    this.webhookService.getEndpoints().subscribe(res => this.endpoints = res);
  }

  addEndpoint(): void {
    const events = this.newEvents.split(',').map(e => e.trim()).filter(e => e);
    this.webhookService.createEndpoint(this.newUrl, events).subscribe(() => {
      this.showAddForm = false;
      this.newUrl = '';
      this.newEvents = '';
      this.loadEndpoints();
    });
  }

  deleteEndpoint(id: string): void {
    this.webhookService.deleteEndpoint(id).subscribe(() => this.loadEndpoints());
  }

  toggleEndpoint(endpoint: WebhookEndpoint): void {
    this.webhookService.toggleEndpoint(endpoint.id).subscribe(res => {
      endpoint.active = res.active;
    });
  }

  loadDeliveries(endpointId: string): void {
    this.webhookService.getDeliveries(endpointId).subscribe(res => {
      this.deliveriesMap[endpointId] = res.content;
    });
  }
}
