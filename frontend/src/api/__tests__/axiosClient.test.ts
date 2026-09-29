/**
 * Tests for axiosClient functionality
 * Verifies JWT injection and 401 error handling
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import MockAdapter from 'axios-mock-adapter';
import axiosClient from '../axiosClient';

// Mock window.location
const mockLocation = {
  pathname: '/',
  href: '',
};

Object.defineProperty(window, 'location', {
  value: mockLocation,
  writable: true,
});

describe('axiosClient', () => {
  let mock: MockAdapter;

  beforeEach(() => {
    mock = new MockAdapter(axiosClient);
    // Clear localStorage before each test
    localStorage.clear();
    // Reset location mock
    mockLocation.pathname = '/';
    mockLocation.href = '';
  });

  afterEach(() => {
    mock.restore();
  });

  describe('Request interceptor', () => {
    it('should inject Authorization header when token is present in localStorage', async () => {
      // Arrange
      const token = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test';
      localStorage.setItem('campuslab_token', token);
      
      mock.onGet('/test').reply((config) => {
        // Assert that Authorization header was injected
        expect(config.headers?.Authorization).toBe(`Bearer ${token}`);
        return [200, { success: true }];
      });

      // Act
      await axiosClient.get('/test');
    });

    it('should not inject Authorization header when token is not present', async () => {
      // Arrange - no token in localStorage
      
      mock.onGet('/test').reply((config) => {
        // Assert that Authorization header was not injected
        expect(config.headers?.Authorization).toBeUndefined();
        return [200, { success: true }];
      });

      // Act
      await axiosClient.get('/test');
    });
  });

  describe('Response interceptor', () => {
    it('should clear localStorage and redirect on 401 error', async () => {
      // Arrange
      localStorage.setItem('campuslab_token', 'some-token');
      localStorage.setItem('campuslab_user', JSON.stringify({ id: '1', name: 'Test' }));
      mockLocation.pathname = '/laboratories';
      
      mock.onGet('/test').reply(401, {
        timestamp: '2025-01-01T10:00:00Z',
        status: 401,
        code: 'AUTHENTICATION_REQUIRED',
        message: 'Token expired',
        path: '/test'
      });

      // Act & Assert
      await expect(axiosClient.get('/test')).rejects.toThrow();
      
      // Verify localStorage was cleared
      expect(localStorage.getItem('campuslab_token')).toBeNull();
      expect(localStorage.getItem('campuslab_user')).toBeNull();
      
      // Verify redirect was attempted
      expect(mockLocation.href).toBe('/login');
    });

    it('should not redirect when already on login page', async () => {
      // Arrange
      mockLocation.pathname = '/login';
      
      mock.onPost('/api/auth/login').reply(401, {
        timestamp: '2025-01-01T10:00:00Z',
        status: 401,
        code: 'AUTHENTICATION_REQUIRED',
        message: 'Invalid credentials',
        path: '/api/auth/login'
      });

      // Act & Assert
      await expect(axiosClient.post('/api/auth/login')).rejects.toThrow();
      
      // Verify no redirect was attempted (href should remain empty)
      expect(mockLocation.href).toBe('');
    });

    it('should propagate non-401 errors without clearing state', async () => {
      // Arrange
      localStorage.setItem('campuslab_token', 'some-token');
      
      mock.onGet('/test').reply(404, {
        timestamp: '2025-01-01T10:00:00Z',
        status: 404,
        code: 'RESOURCE_NOT_FOUND',
        message: 'Resource not found',
        path: '/test'
      });

      // Act & Assert
      await expect(axiosClient.get('/test')).rejects.toThrow();
      
      // Verify localStorage was NOT cleared
      expect(localStorage.getItem('campuslab_token')).toBe('some-token');
      expect(mockLocation.href).toBe('');
    });
  });

  describe('Configuration', () => {
    it('should have correct base configuration', () => {
      expect(axiosClient.defaults.baseURL).toBe('http://localhost:8080');
      expect(axiosClient.defaults.headers['Content-Type']).toBe('application/json');
      expect(axiosClient.defaults.timeout).toBe(10000);
    });
  });
});