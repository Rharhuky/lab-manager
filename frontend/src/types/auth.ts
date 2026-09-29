/**
 * Authentication-related TypeScript interfaces for the CampusLab frontend
 * Corresponds to backend DTOs and JWT structure
 */

export type UserRole = 'ALUNO' | 'PROFESSOR';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface UserSummary {
  id: string;
  name: string;
  email: string;
  role: UserRole;
}

export interface LoginResponse {
  token: string;
  type: string; // Always "Bearer"
  expiresIn: number; // Expiration time in seconds
  user: UserSummary;
}

export interface AuthState {
  token: string | null;
  user: UserSummary | null;
  isAuthenticated: boolean;
}