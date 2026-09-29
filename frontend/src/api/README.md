# CampusLab Frontend API Client

This directory contains the HTTP client configuration and API utilities for the CampusLab frontend.

## axiosClient.ts

The centralized HTTP client provides:

### Automatic JWT Token Injection
- Reads JWT token from `localStorage.getItem('campuslab_token')`
- Automatically adds `Authorization: Bearer <token>` header to all requests
- No manual token management needed in components

### Automatic 401 Error Handling
- Detects expired or invalid JWT tokens (HTTP 401)
- Automatically clears authentication state from localStorage
- Redirects user to `/login` page
- Prevents redirect loops when already on login page

### Configuration
- Base URL: `process.env.VITE_API_BASE_URL` or `http://localhost:8080`
- Timeout: 10 seconds
- Content-Type: `application/json`

## Usage

```typescript
import axiosClient from './api/axiosClient';

// All requests automatically include JWT token if available
const response = await axiosClient.get('/api/laboratories');

// 401 errors are handled automatically - user gets redirected to login
const laboratoryData = await axiosClient.post('/api/laboratories', data);
```

## Environment Variables

- `VITE_API_BASE_URL`: Backend API base URL (defaults to `http://localhost:8080`)

## Requirements Compliance

- **Requirement 1.6**: JWT token storage and injection in Authorization header
- **Requirement 2.1**: HTTP 401 handling with auth state clearing and login redirect