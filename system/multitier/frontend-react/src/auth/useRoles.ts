import { useMemo } from 'react';
import { useAuth } from 'react-oidc-context';
import { ADMIN_ROLE, getRolesFromToken } from './roles';

export function useRoles() {
  const token = useAuth().user?.access_token;
  return useMemo(() => {
    const roles = getRolesFromToken(token);
    return { roles, hasRole: (role: string) => roles.includes(role), isAdmin: roles.includes(ADMIN_ROLE) };
  }, [token]);
}
