import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { TranslateModule } from '@ngx-translate/core';
import { VehicleService } from '../shared/services/vehicle.service';
import { Vehicle, ScrapedTruckData } from '../shared/models/vehicle.model';
import { Truck1ImportDialogComponent } from './truck1-import-dialog.component';

@Component({
  selector: 'app-vehicles',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatCardModule, MatTableModule,
    MatButtonModule, MatIconModule, MatFormFieldModule, MatInputModule, MatSelectModule,
    MatExpansionModule, MatDialogModule, TranslateModule
  ],
  template: `
    <div class="vehicles-container">
      <div class="header">
        <h1>{{ 'vehicles.title' | translate }}</h1>
        <div class="header-actions">
          <button mat-raised-button color="accent" (click)="openImportDialog()">
            <mat-icon>cloud_download</mat-icon>
            {{ 'vehicles.import_from_truck1' | translate }}
          </button>
          <button mat-raised-button color="primary" (click)="showForm = !showForm">
            <mat-icon>{{ showForm ? 'close' : 'add' }}</mat-icon>
            {{ showForm ? ('vehicles.cancel' | translate) : ('vehicles.add' | translate) }}
          </button>
        </div>
      </div>

      @if (showForm) {
        <mat-card class="form-card">
          <mat-card-content>
            <form [formGroup]="form" (ngSubmit)="onSubmit()">
              <div class="form-grid">
                <mat-form-field appearance="outline">
                  <mat-label>{{ 'vehicles.name' | translate }}</mat-label>
                  <input matInput formControlName="name" />
                </mat-form-field>
                <mat-form-field appearance="outline">
                  <mat-label>{{ 'vehicles.fuel_type' | translate }}</mat-label>
                  <mat-select formControlName="fuelType">
                    <mat-option value="DIESEL">{{ 'vehicles.diesel' | translate }}</mat-option>
                    <mat-option value="PETROL">{{ 'vehicles.petrol' | translate }}</mat-option>
                    <mat-option value="LNG">LNG</mat-option>
                    <mat-option value="CNG">CNG</mat-option>
                  </mat-select>
                </mat-form-field>
                <mat-form-field appearance="outline">
                  <mat-label>{{ 'vehicles.consumption_loaded' | translate }}</mat-label>
                  <input matInput formControlName="consumptionLoaded" type="number" />
                </mat-form-field>
                <mat-form-field appearance="outline">
                  <mat-label>{{ 'vehicles.consumption_empty' | translate }}</mat-label>
                  <input matInput formControlName="consumptionEmpty" type="number" />
                </mat-form-field>
                <mat-form-field appearance="outline">
                  <mat-label>{{ 'vehicles.tank_capacity' | translate }}</mat-label>
                  <input matInput formControlName="tankCapacity" type="number" />
                </mat-form-field>
                <mat-form-field appearance="outline">
                  <mat-label>{{ 'vehicles.euro_class' | translate }}</mat-label>
                  <mat-select formControlName="euroClass">
                    <mat-option value="EURO_3">Euro 3</mat-option>
                    <mat-option value="EURO_4">Euro 4</mat-option>
                    <mat-option value="EURO_5">Euro 5</mat-option>
                    <mat-option value="EURO_6">Euro 6</mat-option>
                  </mat-select>
                </mat-form-field>
              </div>
              <mat-accordion>
                <mat-expansion-panel>
                  <mat-expansion-panel-header>
                    <mat-panel-title>{{ 'vehicles_extra.technical_specs' | translate }}</mat-panel-title>
                  </mat-expansion-panel-header>
                  <div class="form-grid">
                    <mat-form-field appearance="outline">
                      <mat-label>{{ 'vehicles.power_hp' | translate }}</mat-label>
                      <input matInput formControlName="powerHp" type="number" />
                    </mat-form-field>
                    <mat-form-field appearance="outline">
                      <mat-label>{{ 'vehicles.axle_config' | translate }}</mat-label>
                      <input matInput formControlName="axleConfiguration" placeholder="4x2, 6x4..." />
                    </mat-form-field>
                    <mat-form-field appearance="outline">
                      <mat-label>{{ 'vehicles.gross_weight' | translate }}</mat-label>
                      <input matInput formControlName="grossWeight" type="number" step="0.1" />
                    </mat-form-field>
                    <mat-form-field appearance="outline">
                      <mat-label>{{ 'vehicles.gearbox' | translate }}</mat-label>
                      <mat-select formControlName="gearbox">
                        <mat-option value="">-</mat-option>
                        <mat-option value="automatic">{{ 'vehicles.automatic' | translate }}</mat-option>
                        <mat-option value="manual">{{ 'vehicles.manual' | translate }}</mat-option>
                      </mat-select>
                    </mat-form-field>
                    <mat-form-field appearance="outline">
                      <mat-label>{{ 'vehicles.suspension' | translate }}</mat-label>
                      <input matInput formControlName="suspension" placeholder="air, spring..." />
                    </mat-form-field>
                    <mat-form-field appearance="outline">
                      <mat-label>{{ 'vehicles.displacement_cc' | translate }}</mat-label>
                      <input matInput formControlName="displacementCc" type="number" />
                    </mat-form-field>
                  </div>
                </mat-expansion-panel>
                <mat-expansion-panel>
                  <mat-expansion-panel-header>
                    <mat-panel-title>{{ 'vehicles_extra.additional_costs' | translate }}</mat-panel-title>
                  </mat-expansion-panel-header>
                  <div class="form-grid">
                    <mat-form-field appearance="outline">
                      <mat-label>{{ 'vehicles_extra.maintenance_per_km' | translate }}</mat-label>
                      <input matInput formControlName="maintenanceCostPerKm" type="number" step="0.001" />
                    </mat-form-field>
                    <mat-form-field appearance="outline">
                      <mat-label>{{ 'vehicles_extra.tire_per_km' | translate }}</mat-label>
                      <input matInput formControlName="tireCostPerKm" type="number" step="0.001" />
                    </mat-form-field>
                    <mat-form-field appearance="outline">
                      <mat-label>{{ 'vehicles_extra.depreciation_per_km' | translate }}</mat-label>
                      <input matInput formControlName="depreciationPerKm" type="number" step="0.001" />
                    </mat-form-field>
                    <mat-form-field appearance="outline">
                      <mat-label>{{ 'vehicles_extra.insurance_per_day' | translate }}</mat-label>
                      <input matInput formControlName="insurancePerDay" type="number" step="0.01" />
                    </mat-form-field>
                  </div>
                </mat-expansion-panel>
              </mat-accordion>
              <button mat-raised-button color="primary" type="submit" [disabled]="form.invalid" style="margin-top: 16px">
                {{ editingId ? ('vehicles.update' | translate) : ('vehicles.add_btn' | translate) }}
              </button>
            </form>
          </mat-card-content>
        </mat-card>
      }

      <mat-card>
        <table mat-table [dataSource]="vehicles" class="full-width">
          <ng-container matColumnDef="name">
            <th mat-header-cell *matHeaderCellDef>{{ 'vehicles.name' | translate }}</th>
            <td mat-cell *matCellDef="let v">
              {{ v.name }}
              @if (v.source === 'TRUCK1') {
                <mat-icon style="font-size: 14px; height: 14px; width: 14px; vertical-align: middle; color: #666;" matTooltip="Imported from Truck1">cloud_done</mat-icon>
              }
            </td>
          </ng-container>
          <ng-container matColumnDef="fuelType">
            <th mat-header-cell *matHeaderCellDef>{{ 'vehicles.col_fuel' | translate }}</th>
            <td mat-cell *matCellDef="let v">{{ v.fuelType }}</td>
          </ng-container>
          <ng-container matColumnDef="consumptionLoaded">
            <th mat-header-cell *matHeaderCellDef>{{ 'vehicles.col_consumption_loaded' | translate }}</th>
            <td mat-cell *matCellDef="let v">{{ v.consumptionLoaded }} l/100km</td>
          </ng-container>
          <ng-container matColumnDef="consumptionEmpty">
            <th mat-header-cell *matHeaderCellDef>{{ 'vehicles.col_consumption_empty' | translate }}</th>
            <td mat-cell *matCellDef="let v">{{ v.consumptionEmpty }} l/100km</td>
          </ng-container>
          <ng-container matColumnDef="euroClass">
            <th mat-header-cell *matHeaderCellDef>{{ 'vehicles.euro_class' | translate }}</th>
            <td mat-cell *matCellDef="let v">{{ v.euroClass }}</td>
          </ng-container>
          <ng-container matColumnDef="actions">
            <th mat-header-cell *matHeaderCellDef>{{ 'vehicles.actions' | translate }}</th>
            <td mat-cell *matCellDef="let v">
              <button mat-icon-button color="primary" (click)="edit(v)">
                <mat-icon>edit</mat-icon>
              </button>
              <button mat-icon-button color="warn" (click)="delete(v)">
                <mat-icon>delete</mat-icon>
              </button>
            </td>
          </ng-container>

          <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
          <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
        </table>
      </mat-card>
    </div>
  `,
  styleUrl: './vehicles.component.scss'
})
export class VehiclesComponent implements OnInit {
  private fb = inject(FormBuilder);
  private vehicleService = inject(VehicleService);
  private dialog = inject(MatDialog);

  vehicles: Vehicle[] = [];
  showForm = false;
  editingId: string | null = null;
  displayedColumns = ['name', 'fuelType', 'consumptionLoaded', 'consumptionEmpty', 'euroClass', 'actions'];

  form = this.fb.group({
    name: ['', Validators.required],
    fuelType: ['DIESEL', Validators.required],
    consumptionLoaded: [null as number | null, Validators.required],
    consumptionEmpty: [null as number | null, Validators.required],
    tankCapacity: [null as number | null],
    euroClass: ['EURO_6'],
    maintenanceCostPerKm: [null as number | null],
    tireCostPerKm: [null as number | null],
    depreciationPerKm: [null as number | null],
    insurancePerDay: [null as number | null],
    powerHp: [null as number | null],
    axleConfiguration: [''],
    grossWeight: [null as number | null],
    gearbox: [''],
    suspension: [''],
    displacementCc: [null as number | null]
  });

  ngOnInit(): void {
    this.loadVehicles();
  }

  loadVehicles(): void {
    this.vehicleService.getVehicles().subscribe(v => this.vehicles = v.filter(x => x.active));
  }

  onSubmit(): void {
    if (this.form.invalid) return;
    const req = this.form.value as any;
    const obs = this.editingId
      ? this.vehicleService.updateVehicle(this.editingId, req)
      : this.vehicleService.createVehicle(req);
    obs.subscribe(() => {
      this.loadVehicles();
      this.showForm = false;
      this.editingId = null;
      this.form.reset({ fuelType: 'DIESEL', euroClass: 'EURO_6' });
    });
  }

  edit(v: Vehicle): void {
    this.editingId = v.id;
    this.showForm = true;
    this.form.patchValue(v);
  }

  delete(v: Vehicle): void {
    this.vehicleService.deleteVehicle(v.id).subscribe(() => this.loadVehicles());
  }

  openImportDialog(): void {
    const dialogRef = this.dialog.open(Truck1ImportDialogComponent, {
      width: '700px',
      maxHeight: '90vh'
    });

    dialogRef.afterClosed().subscribe((truck: ScrapedTruckData | undefined) => {
      if (!truck) return;
      // Pre-fill the form with scraped data
      this.editingId = null;
      this.showForm = true;
      this.form.patchValue({
        name: truck.name || '',
        fuelType: truck.fuelType || 'DIESEL',
        tankCapacity: truck.tankCapacity || null,
        euroClass: truck.euroClass || 'EURO_6',
        powerHp: truck.powerHp || null,
        axleConfiguration: truck.axleConfiguration || '',
        grossWeight: truck.grossWeight || null,
        gearbox: truck.gearbox || '',
        suspension: truck.suspension || '',
        displacementCc: truck.displacementCc || null,
        consumptionLoaded: null,
        consumptionEmpty: null
      });
    });
  }
}
