/**
 * Main types export file for the CampusLab frontend
 * Provides centralized access to all TypeScript interfaces
 */

// Authentication types
export type { 
  UserRole, 
  LoginRequest, 
  LoginResponse, 
  UserSummary, 
  AuthState 
} from './auth';

// Laboratory types
export type { 
  Laboratory, 
  LaboratoryDetail, 
  LaboratoryRequest, 
  LaboratoryResponse 
} from './laboratory';

// Reservation types
export type { 
  Reservation, 
  ReservationRequest, 
  ReservationResponse 
} from './reservation';

// API types
export type { 
  ErrorResponse 
} from './api';