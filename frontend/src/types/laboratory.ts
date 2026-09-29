/**
 * Laboratory-related TypeScript interfaces for the CampusLab frontend
 * Corresponds to backend DTOs and entities
 */

import { Reservation } from './reservation';

export interface LaboratoryRequest {
  name: string; // max 100 characters
  block: string; // max 20 characters
}

export interface Laboratory {
  id: string;
  name: string;
  block: string;
  reserved: boolean; // Calculated field indicating if there are future reservations
  createdAt: string; // ISO 8601 string
  updatedAt: string; // ISO 8601 string
}

// Alias for consistency with design document naming
export interface LaboratoryResponse extends Laboratory {}

export interface LaboratoryDetail extends Laboratory {
  reservations: Reservation[]; // List of reservations for this laboratory
}