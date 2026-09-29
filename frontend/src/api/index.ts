/**
 * API module exports for CampusLab frontend
 * Central export point for all API functions
 */

export { default as axiosClient } from './axiosClient';
export { authApi } from './authApi';
export { laboratoryApi } from './laboratoryApi';
export { reservationApi } from './reservationApi';

// Re-export for convenience
export default {
  auth: authApi,
  laboratory: laboratoryApi,
  reservation: reservationApi,
};