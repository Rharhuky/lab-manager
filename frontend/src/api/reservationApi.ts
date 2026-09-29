/**
 * Reservation API functions for CampusLab
 * Handles CRUD operations for laboratory reservations
 */

import axiosClient from './axiosClient';
import { Reservation, ReservationRequest, ReservationResponse } from '../types/reservation';

export const reservationApi = {
  /**
   * Retrieves all reservations for a specific laboratory, ordered by startAt ascending
   * GET /api/laboratories/{laboratoryId}/reservations
   * 
   * @param laboratoryId - Laboratory UUID
   * @returns Promise resolving to array of ReservationResponse ordered by startAt
   * @throws AxiosError with status 404 for non-existent laboratory
   * 
   * Requirements: 5.1
   */
  async getByLaboratory(laboratoryId: string): Promise<ReservationResponse[]> {
    const response = await axiosClient.get<ReservationResponse[]>(`/api/laboratories/${laboratoryId}/reservations`);
    return response.data;
  },

  /**
   * Retrieves detailed information for a specific reservation
   * GET /api/reservations/{id}
   * 
   * @param id - Reservation UUID
   * @returns Promise resolving to ReservationResponse with all reservation data
   * @throws AxiosError with status 404 for non-existent reservation
   * 
   * Requirements: 5.2
   */
  async getById(id: string): Promise<ReservationResponse> {
    const response = await axiosClient.get<ReservationResponse>(`/api/reservations/${id}`);
    return response.data;
  },

  /**
   * Creates a new reservation for a laboratory
   * POST /api/laboratories/{laboratoryId}/reservations
   * Requires PROFESSOR role
   * 
   * @param laboratoryId - Laboratory UUID where the reservation will be created
   * @param reservationRequest - Reservation data with startAt and endAt
   * @returns Promise resolving to created ReservationResponse with generated ID
   * @throws AxiosError with status 404 for non-existent laboratory, 400 for invalid period, 409 for time conflicts, 403 for insufficient permissions
   * 
   * Requirements: 6.1, 6.4, 6.6, 6.7, 7.1, 7.2, 7.6
   */
  async create(laboratoryId: string, reservationRequest: ReservationRequest): Promise<ReservationResponse> {
    const response = await axiosClient.post<ReservationResponse>(
      `/api/laboratories/${laboratoryId}/reservations`, 
      reservationRequest
    );
    return response.data;
  },

  /**
   * Updates an existing reservation
   * PUT /api/reservations/{id}
   * Requires PROFESSOR role
   * 
   * @param id - Reservation UUID to update
   * @param reservationRequest - Updated reservation data
   * @returns Promise resolving to updated ReservationResponse
   * @throws AxiosError with status 404 for non-existent reservation, 400 for invalid period, 409 for time conflicts, 403 for insufficient permissions
   * 
   * Requirements: 6.2, 6.5, 6.6, 6.7, 7.1, 7.2, 7.3, 7.6
   */
  async update(id: string, reservationRequest: ReservationRequest): Promise<ReservationResponse> {
    const response = await axiosClient.put<ReservationResponse>(`/api/reservations/${id}`, reservationRequest);
    return response.data;
  },

  /**
   * Deletes a reservation
   * DELETE /api/reservations/{id}
   * Requires PROFESSOR role
   * 
   * @param id - Reservation UUID to delete
   * @returns Promise resolving to void (HTTP 204)
   * @throws AxiosError with status 404 for non-existent reservation, 403 for insufficient permissions
   * 
   * Requirements: 6.3, 6.5
   */
  async remove(id: string): Promise<void> {
    await axiosClient.delete(`/api/reservations/${id}`);
  },
};

export default reservationApi;