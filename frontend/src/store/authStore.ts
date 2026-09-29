import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { LoginResponse, UserSummary, UserRole } from '../types/auth';

interface AuthState {
  token: string | null;
  user: UserSummary | null;
  isAuthenticated: boolean;
}

interface AuthActions {
  setAuth: (loginResponse: LoginResponse) => void;
  clearAuth: () => void;
  getUserRole: () => UserRole | null;
}

type AuthStore = AuthState & AuthActions;

/**
 * Decode JWT token to extract user role
 * This is a client-side utility for UI decisions only
 * Security validation is always done server-side
 */
const extractRoleFromToken = (token: string): UserRole | null => {
  try {
    // Validate token format
    if (!token || typeof token !== 'string') return null;
    
    // JWT structure: header.payload.signature
    const parts = token.split('.');
    if (parts.length !== 3) return null;
    
    // Decode payload (base64url)
    const payload = parts[1];
    if (!payload) return null;
    
    // Add padding if needed for base64url decoding
    const paddedPayload = payload + '='.repeat((4 - payload.length % 4) % 4);
    const decodedPayload = atob(paddedPayload.replace(/-/g, '+').replace(/_/g, '/'));
    
    const claims = JSON.parse(decodedPayload);
    
    // Validate role value
    const role = claims?.role;
    if (role === 'ALUNO' || role === 'PROFESSOR') {
      return role;
    }
    
    return null;
  } catch (error) {
    console.warn('Failed to decode JWT token:', error);
    return null;
  }
};

export const useAuthStore = create<AuthStore>()(
  persist(
    (set, get) => ({
      // Initial state
      token: null,
      user: null,
      isAuthenticated: false,

      // Actions
      setAuth: (loginResponse: LoginResponse) => {
        // Validate the login response structure
        if (!loginResponse?.token || !loginResponse?.user) {
          console.error('Invalid login response provided to setAuth');
          return;
        }

        const { token, user } = loginResponse;
        
        set({
          token,
          user,
          isAuthenticated: true,
        });
      },

      clearAuth: () => {
        set({
          token: null,
          user: null,
          isAuthenticated: false,
        });
      },

      getUserRole: (): UserRole | null => {
        const { token, user } = get();
        
        // Primary source: user object from login response
        if (user?.role) {
          return user.role;
        }
        
        // Fallback: extract from JWT token
        if (token) {
          return extractRoleFromToken(token);
        }
        
        return null;
      },
    }),
    {
      name: 'campus-lab-auth', // localStorage key
      // Only persist essential auth data
      // Functions like getUserRole are derived and don't need persistence
      partialize: (state) => ({
        token: state.token,
        user: state.user,
        isAuthenticated: state.isAuthenticated,
      }),
      // Optional: Add version for future state migrations
      version: 1,
    }
  )
);