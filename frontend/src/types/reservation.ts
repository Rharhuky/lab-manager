/**
 * Reservation-related TypeScript interfaces for the CampusLab frontend
 * Corresponds to backend DTOs and entities
 */

export interface ReservationRequest {
  startAt: string; // ISO 8601 string (Instant from backend)
  endAt: string; // ISO 8601 string (Instant from backend)
}

export interface Reservation {
  id: string;
  laboratoryId: string; // UUID as string
  userId: string; // UUID as string
  startAt: string; // ISO 8601 string
  endAt: string; // ISO 8601 string
  createdAt: string; // ISO 8601 string
}

// Alias for consistency with design document naming
export interface ReservationResponse extends Reservation {}