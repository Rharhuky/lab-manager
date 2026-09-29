/**
 * Store exports for CampusLab frontend
 * Centralized exports for all store modules
 */

export { useAuthStore } from './authStore';

// Re-export auth types for convenience
export type { UserRole, LoginResponse, AuthState } from '../types/auth';