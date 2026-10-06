export const ADMIN_ROLE = 'ADMIN';
export const CUSTOMER_ROLE = 'CUSTOMER';

function decodeBase64Url(value: string): string {
  const padded = value.replace(/-/g, '+').replace(/_/g, '/').padEnd(Math.ceil(value.length / 4) * 4, '=');
  const bytes = Uint8Array.from(atob(padded), (c) => c.codePointAt(0) ?? 0);
  return new TextDecoder().decode(bytes);
}

/** Reads `realm_access.roles` from a JWT payload; returns no roles when the token is missing or malformed. */
export function getRolesFromToken(token: string | undefined): string[] {
  if (!token) return [];
  try {
    const payload = JSON.parse(decodeBase64Url(token.split('.')[1] ?? '')) as { realm_access?: { roles?: unknown } };
    const roles = payload.realm_access?.roles;
    return Array.isArray(roles) ? roles.filter((r): r is string => typeof r === 'string') : [];
  } catch {
    return [];
  }
}
