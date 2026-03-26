export interface Vehicle {
  id: string;
  name: string;
  fuelType: string;
  consumptionLoaded: number;
  consumptionEmpty: number;
  tankCapacity: number;
  euroClass: string;
  active: boolean;
  maintenanceCostPerKm?: number;
  tireCostPerKm?: number;
  depreciationPerKm?: number;
  insurancePerDay?: number;
}

export interface VehicleRequest {
  name: string;
  fuelType: string;
  consumptionLoaded: number;
  consumptionEmpty: number;
  tankCapacity?: number;
  euroClass?: string;
  maintenanceCostPerKm?: number;
  tireCostPerKm?: number;
  depreciationPerKm?: number;
  insurancePerDay?: number;
}
