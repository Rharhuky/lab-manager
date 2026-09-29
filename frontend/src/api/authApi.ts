/**
 * Authentication API functions for CampusLab
 * Handles login operations and JWT token management
 */

import axiosClient from './axiosClient';
import { LoginRequest, LoginResponse } from '../types/auth';

export const authApi = {
  /**
   * Authenticates user with email and password
   * POST /api/auth/login
   * 
   * @param loginRequest - User credentials
   * @returns Promise resolving to LoginResponse with JWT token and user data
   * @throws AxiosError with ErrorResponse data for validation/auth failures
   * 
   * Requirements: 1.1, 1.3
   */
  async login(loginRequest: LoginRequest): Promise<LoginResponse> {
    const response = await axiosClient.post<LoginResponse>('/api/auth/login', loginRequest);
    return response.data;
  },
};

export default authApi;