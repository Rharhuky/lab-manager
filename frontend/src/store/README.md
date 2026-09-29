# AuthStore Implementation

This directory contains the authentication store implementation for the CampusLab frontend.

## Usage

### Basic Usage in Components

```typescript
import { useAuthStore } from './store/authStore';

function MyComponent() {
  const { token, user, isAuthenticated, setAuth, clearAuth, getUserRole } = useAuthStore();
  
  // Check if user is authenticated
  if (!isAuthenticated) {
    return <div>Please log in</div>;
  }
  
  // Get user role for conditional rendering
  const userRole = getUserRole();
  
  return (
    <div>
      <h1>Welcome, {user?.name}!</h1>
      {userRole === 'PROFESSOR' && (
        <button>Admin Action</button>
      )}
      <button onClick={clearAuth}>Logout</button>
    </div>
  );
}
```

### Setting Authentication State

```typescript
import { useAuthStore } from './store/authStore';
import type { LoginResponse } from './types/auth';

function LoginComponent() {
  const setAuth = useAuthStore(state => state.setAuth);
  
  const handleLogin = async (email: string, password: string) => {
    try {
      const loginResponse: LoginResponse = await authApi.login({ email, password });
      setAuth(loginResponse);
      // User is now authenticated, redirect to protected route
      navigate('/laboratories');
    } catch (error) {
      console.error('Login failed:', error);
    }
  };
  
  // ... rest of component
}
```

### Role-based Guards

```typescript
import { useAuthStore } from './store/authStore';

function ProtectedAction() {
  const getUserRole = useAuthStore(state => state.getUserRole);
  
  const userRole = getUserRole();
  
  if (userRole !== 'PROFESSOR') {
    return null; // Don't render for non-professors
  }
  
  return (
    <button onClick={handleAdminAction}>
      Create Laboratory
    </button>
  );
}
```

## Features

- ✅ **State Management**: Manages `token`, `user`, and `isAuthenticated` state
- ✅ **Persistence**: Automatically persists auth state to localStorage
- ✅ **Role Extraction**: `getUserRole()` method extracts role from user object or JWT token
- ✅ **Type Safety**: Full TypeScript support with proper interfaces
- ✅ **Clean API**: Simple `setAuth()` and `clearAuth()` actions

## Implementation Details

### LocalStorage Persistence

The store automatically persists authentication state to localStorage with the key `'campus-lab-auth'`. This ensures the user remains logged in across browser refreshes.

### Role Extraction Strategy

The `getUserRole()` method uses a two-tier approach:

1. **Primary**: Returns role from the `user` object (from login response)
2. **Fallback**: Extracts role from JWT token payload if user object is unavailable

This provides flexibility while maintaining security (server-side validation is still required).

### JWT Token Handling

The store includes a utility function to safely decode JWT tokens for client-side UI decisions. This is purely for user experience - all security validation happens server-side.

## Testing

Run the test suite to verify functionality:

```bash
npm test authStore.test.ts
```

The tests cover:
- Initial state validation
- Authentication state management
- Role extraction from user object and JWT
- LocalStorage persistence
- Error handling for invalid tokens