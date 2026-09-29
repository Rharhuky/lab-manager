/**
 * Unit tests for authStore implementation
 */
import { vi, describe, it, expect, beforeEach } from 'vitest';

// Provide a proper localStorage mock compatible with Zustand persist
const storage: Record<string, string> = {};
const localStorageMock = {
  getItem: vi.fn((key: string) => storage[key] ?? null),
  setItem: vi.fn((key: string, value: string) => { storage[key] = value; }),
  removeItem: vi.fn((key: string) => { delete storage[key]; }),
  clear: vi.fn(() => { Object.keys(storage).forEach((k) => delete storage[k]); }),
  length: 0,
  key: vi.fn(),
};

vi.stubGlobal('localStorage', localStorageMock);

// Import AFTER stubbing localStorage so Zustand picks up the mock
const { useAuthStore } = await import('./authStore');
import type { LoginResponse } from '../types/auth';

const makeLoginResponse = (role: 'ALUNO' | 'PROFESSOR' = 'PROFESSOR'): LoginResponse => ({
  token: 'mock-jwt-token',
  type: 'Bearer',
  expiresIn: 3600,
  user: {
    id: '123e4567-e89b-12d3-a456-426614174000',
    name: 'Test User',
    email: 'test@example.com',
    role,
  },
});

describe('AuthStore', () => {
  beforeEach(() => {
    useAuthStore.getState().clearAuth();
    vi.clearAllMocks();
  });

  describe('Initial state', () => {
    it('should have correct initial state', () => {
      const state = useAuthStore.getState();
      expect(state.token).toBe(null);
      expect(state.user).toBe(null);
      expect(state.isAuthenticated).toBe(false);
      expect(state.getUserRole()).toBe(null);
    });
  });

  describe('setAuth', () => {
    it('should set authentication state correctly', () => {
      const loginResponse = makeLoginResponse('PROFESSOR');
      useAuthStore.getState().setAuth(loginResponse);

      const s = useAuthStore.getState();
      expect(s.token).toBe('mock-jwt-token');
      expect(s.user).toEqual(loginResponse.user);
      expect(s.isAuthenticated).toBe(true);
      expect(s.getUserRole()).toBe('PROFESSOR');
    });
  });

  describe('clearAuth', () => {
    it('should clear authentication state', () => {
      useAuthStore.getState().setAuth(makeLoginResponse());
      useAuthStore.getState().clearAuth();

      const s = useAuthStore.getState();
      expect(s.token).toBe(null);
      expect(s.user).toBe(null);
      expect(s.isAuthenticated).toBe(false);
    });
  });

  describe('getUserRole', () => {
    it('should return role from user object', () => {
      useAuthStore.getState().setAuth(makeLoginResponse('ALUNO'));
      expect(useAuthStore.getState().getUserRole()).toBe('ALUNO');
    });

    it('should return null when no token and no user', () => {
      expect(useAuthStore.getState().getUserRole()).toBe(null);
    });
  });
});
