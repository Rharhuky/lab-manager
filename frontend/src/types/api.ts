/**
 * API-related TypeScript interfaces for the CampusLab frontend
 * Corresponds to backend ErrorResponse DTO and standardized error format
 */

export interface ErrorResponse {
  timestamp: string; // ISO 8601 string in UTC
  status: number; // HTTP status code (400, 401, 403, 404, 409, 500, etc.)
  code: string; // Application-level error code (distinct from HTTP status)
  message: string; // Human-readable error message (max 512 characters)
  path: string; // API endpoint path that generated the error
}