/**
 * Centralized HTTP client for the CampusLab frontend
 * 
 * Provides:
 * - Automatic JWT token injection via request interceptor
 * - Automatic 401 handling with auth state clearing and redirect
 * - Configured base URL for the backend API
 * 
 * Requirements: 1.6, 2.1
 */

import axios, { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse, AxiosError } from 'axios';

// Base URL for the backend API - adjust this to match your backend server
const API_BASE_URL = (import.meta as { env?: { VITE_API_BASE_URL?: string } }).env?.VITE_API_BASE_URL ?? 'http://localhost:8080';

/**
 * Axios instance configured for CampusLab API communication
 */
export const axiosClient: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000, // 10 seconds timeout
});

/**
 * Request interceptor to automatically inject JWT token
 * Adds Authorization header when token is available in localStorage
 */
axiosClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // Get token from localStorage (where authStore persists it)
    const token = localStorage.getItem('campuslab_token');
    
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

/**
 * Response interceptor to handle 401 errors (expired/invalid tokens)
 * Clears authentication state and redirects to login page
 */
axiosClient.interceptors.response.use(
  (response: AxiosResponse) => {
    return response;
  },
  (error: AxiosError) => {
    // Handle 401 Unauthorized errors
    if (error.response?.status === 401) {
      // Clear authentication state
      localStorage.removeItem('campuslab_token');
      localStorage.removeItem('campuslab_user');
      
      // Redirect to login page
      // Use window.location to ensure redirect works even if router is not available
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    
    // Propagate error for local handling in components
    return Promise.reject(error);
  }
);

export default axiosClient;