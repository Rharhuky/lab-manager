import type { ReactNode } from 'react';
import type { UserRole } from '../types/auth';
import { useAuthStore } from '../store/authStore';

interface RoleGuardProps {
  requiredRole: UserRole;
  children: ReactNode;
}

/**
 * Renders children only if the authenticated user has the required role.
 * When the role does not match, the children are NOT rendered at all (removed from DOM).
 *
 * Requirements: 10.2, 10.3, 12.1
 */
export function RoleGuard({ requiredRole, children }: RoleGuardProps) {
  const getUserRole = useAuthStore((s) => s.getUserRole);
  const userRole = getUserRole();

  if (userRole !== requiredRole) {
    return null;
  }

  return <>{children}</>;
}

export default RoleGuard;
