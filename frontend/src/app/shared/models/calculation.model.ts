export interface CalculateRequest {
  vehicleId: string;
  originAddress: string;
  destinationAddress: string;
  waypoints?: string[];
  cargoWeightTons?: number;
  orderPrice: number;
  currency?: string;
  isLoaded?: boolean;
  driverDailyRate?: number;
  otherCosts?: number;
  includeReturnTrip?: boolean;
}

export interface LegDetail {
  from: string;
  to: string;
  distanceKm: number;
  estimatedHours: number;
}

export interface TollBreakdown {
  countryCode: string;
  distanceKm: number;
  costPerKm: number;
  cost: number;
}

export interface CalculationResponse {
  id: string;
  origin: string;
  destination: string;
  distanceKm: number;
  estimatedHours: number;
  vehicleName: string;
  cargoWeightT: number;
  orderPrice: number;
  currency: string;
  fuelCost: number;
  tollCost: number;
  driverDailyCost: number;
  otherCosts: number;
  totalCost: number;
  profit: number;
  profitMarginPct: number;
  revenuePerKm: number;
  isProfitable: boolean;
  fuelBreakdown: string;
  includeReturnTrip: boolean;
  returnFuelCost: number;
  co2EmissionsKg: number;
  driverDays: number;
  costPerKm: number;
  maintenanceCost: number;
  tireCost: number;
  depreciationCost: number;
  insuranceCost: number;
  routeIndex?: number;
  routeLabel?: string;
  tollBreakdown?: TollBreakdown[];
  waypoints: string[];
  legs: LegDetail[];
  createdAt: string;
}

export interface RouteAlternative {
  index: number;
  label: string;
  distanceKm: number;
  estimatedHours: number;
  summary: string;
  estimatedFuelCost: number;
  estimatedTotalCost: number;
}

export interface AlternativesResponse {
  alternatives: RouteAlternative[];
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
