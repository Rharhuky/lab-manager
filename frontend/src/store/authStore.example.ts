/**
 * Example usage of authStore in different scenarios
 * This file demonstrates how to integrate the authStore with various frontend components
 */

import { useAuthStore } from './authStore';
import type { LoginResponse } from '../types/auth';

// Example 1: Login flow integration
export const handleSuccessfulLogin = (loginResponse: LoginResponse) => {
  const { setAuth } = useAuthStore.getState();
  
  // Store authentication data (satisfies Requirements 1.6 and 9.5)
  setAuth(loginResponse);
  
  // The token is now persisted in localStorage and available for API calls
  console.log('User authenticated:', useAuthStore.getState().user?.name);
  
  // In a real app, this would trigger navigation to /laboratories
  // navigate('/laboratories');
};

// Example 2: Logout flow
export const handleLogout = () => {
  const { clearAuth } = useAuthStore.getState();
  
  // Clear all authentication state
  clearAuth();
  
  // In a real app, this would trigger navigation to /login
  // navigate('/login');
};

// Example 3: Role-based component rendering
export const ExampleRoleBasedComponent = () => {
  const { isAuthenticated, user, getUserRole } = useAuthStore();
  
  if (!isAuthenticated) {
    return null; // Or redirect to login
  }
  
  const userRole = getUserRole();
  
  return {
    renderForProfessor: userRole === 'PROFESSOR' ? (
      // Professor-only UI elements
      `<button>Create Laboratory</button>`
    ) : null,
    
    renderForStudent: userRole === 'ALUNO' ? (
      // Student-specific UI elements  
      `<div>View-only mode</div>`
    ) : null,
    
    renderForAll: `<div>Welcome, ${user?.name}!</div>`
  };
};

// Example 4: Token availability for HTTP client
export const getAuthToken = (): string | null => {
  const { token, isAuthenticated } = useAuthStore.getState();
  
  // Only return token if user is authenticated
  // This would be used by axiosClient interceptor (task 13.1)
  return isAuthenticated ? token : null;
};

// Example 5: Auth state subscription for reactive UI
export const subscribeToAuthChanges = () => {
  return useAuthStore.subscribe(
    (state) => state.isAuthenticated,
    (isAuthenticated) => {
      console.log('Authentication status changed:', isAuthenticated);
      
      // React to authentication changes
      if (!isAuthenticated) {
        // User logged out, redirect to login
        console.log('Redirecting to login...');
      } else {
        // User logged in, update UI
        console.log('User authenticated, updating UI...');
      }
    }
  );
};

// Example 6: Checking authentication status
export const checkAuthStatus = () => {
  const { isAuthenticated, user, token } = useAuthStore.getState();
  
  return {
    isLoggedIn: isAuthenticated,
    hasValidToken: Boolean(token),
    userRole: user?.role || null,
    userName: user?.name || null,
    needsLogin: !isAuthenticated || !token,
  };
};

// Example 7: Manual token validation (optional)
export const validateStoredAuth = () => {
  const { token, user, isAuthenticated, clearAuth } = useAuthStore.getState();
  
  // Basic validation - in a real app, you might want to verify token expiration
  if (isAuthenticated && (!token || !user)) {
    console.warn('Inconsistent auth state detected, clearing auth');
    clearAuth();
    return false;
  }
  
  return isAuthenticated;
};