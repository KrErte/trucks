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
  axleConfiguration?: string;
  numberOfAxles?: number;
  grossWeight?: number;
  netWeight?: number;
  powerHp?: number;
  displacementCc?: number;
  gearbox?: string;
  suspension?: string;
  source?: string;
  sourceId?: string;
  sourceUrl?: string;
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
  axleConfiguration?: string;
  numberOfAxles?: number;
  grossWeight?: number;
  netWeight?: number;
  powerHp?: number;
  displacementCc?: number;
  gearbox?: string;
  suspension?: string;
  source?: string;
  sourceId?: string;
  sourceUrl?: string;
}

export interface ScrapedTruckData {
  sourceId?: string;
  sourceUrl?: string;
  name?: string;
  brand?: string;
  model?: string;
  year?: number;
  fuelType?: string;
  euroClass?: string;
  price?: number;
  currency?: string;
  axleConfiguration?: string;
  numberOfAxles?: number;
  grossWeight?: number;
  netWeight?: number;
  powerHp?: number;
  displacementCc?: number;
  gearbox?: string;
  suspension?: string;
  tankCapacity?: number;
  mileageKm?: number;
  imageUrl?: string;
  location?: string;
}
