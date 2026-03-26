export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  email: string;
  role: string;
  companyId: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  companyName: string;
  vatNumber?: string;
  country?: string;
}
