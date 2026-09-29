/**
 * Laboratory API functions for CampusLab
 * Handles CRUD operations for laboratories
 */

import axiosClient from './axiosClient';
import { Laboratory, LaboratoryDetail, LaboratoryRequest, LaboratoryResponse } from '../types/laboratory';

export const laboratoryApi = {
  /**
   * Retrieves all laboratories with their basic information and reservation status
   * GET /api/laboratories
   * 
   * @returns Promise resolving to array of LaboratoryResponse with calculated 'reserved' field
   * @throws AxiosError for authentication failures or server errors
   * 
   * Requirements: 3.1, 3.6
   */
  async getAll(): Promise<LaboratoryResponse[]> {
    const response = await axiosClient.get<LaboratoryResponse[]>('/api/laboratories');
    return response.data;
  },

  /**
   * Retrieves detailed laboratory information including associated reservations
   * GET /api/laboratories/{id}
   * 
   * @param id - Laboratory UUID
   * @returns Promise resolving to LaboratoryDetail with reservations array
   * @throws AxiosError with status 404 for non-existent laboratory
   * 
   * Requirements: 3.2
   */
  async getById(id: string): Promise<LaboratoryDetail> {
    const response = await axiosClient.get<LaboratoryDetail>(`/api/laboratories/${id}`);
    return response.data;
  },

  /**
   * Creates a new laboratory
   * POST /api/laboratories
   * Requires PROFESSOR role
   * 
   * @param laboratoryRequest - Laboratory data with name and block
   * @returns Promise resolving to created LaboratoryResponse with generated ID
   * @throws AxiosError with status 400 for validation errors, 409 for duplicate name, 403 for insufficient permissions
   * 
   * Requirements: 4.1, 4.5, 4.7
   */
  async create(laboratoryRequest: LaboratoryRequest): Promise<LaboratoryResponse> {
    const response = await axiosClient.post<LaboratoryResponse>('/api/laboratories', laboratoryRequest);
    return response.data;
  },

  /**
   * Updates an existing laboratory
   * PUT /api/laboratories/{id}
   * Requires PROFESSOR role
   * 
   * @param id - Laboratory UUID to update
   * @param laboratoryRequest - Updated laboratory data
   * @returns Promise resolving to updated LaboratoryResponse
   * @throws AxiosError with status 404 for non-existent laboratory, 400 for validation errors, 409 for duplicate name, 403 for insufficient permissions
   * 
   * Requirements: 4.2, 4.6, 4.7
   */
  async update(id: string, laboratoryRequest: LaboratoryRequest): Promise<LaboratoryResponse> {
    const response = await axiosClient.put<LaboratoryResponse>(`/api/laboratories/${id}`, laboratoryRequest);
    return response.data;
  },

  /**
   * Deletes a laboratory
   * DELETE /api/laboratories/{id}
   * Requires PROFESSOR role
   * 
   * @param id - Laboratory UUID to delete
   * @returns Promise resolving to void (HTTP 204)
   * @throws AxiosError with status 404 for non-existent laboratory, 409 for laboratory with future reservations, 403 for insufficient permissions
   * 
   * Requirements: 4.3, 4.4, 4.6
   */
  async remove(id: string): Promise<void> {
    await axiosClient.delete(`/api/laboratories/${id}`);
  },
};

export default laboratoryApi;