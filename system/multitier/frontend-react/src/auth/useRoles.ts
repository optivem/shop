import { useContext, useMemo } from 'react';
import { AuthContext } from 'react-oidc-context';
import { AuthDisabledContext } from './auth-disabled';
import { ADMIN_ROLE, CUSTOMER_ROLE, getRolesFromToken } from './roles';

export function useRoles() {
  const authDisabled = useContext(AuthDisabledContext);
  const token = useContext(AuthContext)?.user?.access_token;
  return useMemo(() => {
    const roles = getRolesFromToken(token);
    if (authDisabled) return { roles, hasRole: () => true, isAdmin: true, canPlaceOrder: true };
    const isAdmin = roles.includes(ADMIN_ROLE);
    // Admins who are not also customers manage orders but do not place them.
    const canPlaceOrder = !isAdmin || roles.includes(CUSTOMER_ROLE);
    return { roles, hasRole: (role: string) => roles.includes(role), isAdmin, canPlaceOrder };
  }, [token, authDisabled]);
}
