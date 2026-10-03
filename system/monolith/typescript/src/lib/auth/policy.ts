export const ADMIN = 'ADMIN';

export type Decision = 'allow' | 'unauthenticated' | 'forbidden';

export interface Principal {
  roles: readonly string[];
}

const PUBLIC_PATHS = new Set(['/health', '/login', '/login/callback', '/logout']);

const ADMIN_ONLY: readonly ((method: string, path: string) => boolean)[] = [
  (method, path) => method === 'POST' && /^\/api\/orders\/[^/]+\/deliver$/.test(path),
  (_method, path) => path === '/api/coupons',
  (_method, path) => path === '/api/admin' || path.startsWith('/api/admin/'),
  (_method, path) => path === '/admin-coupons',
];

/**
 * The single place that says who may call what. Secure by default: anything not listed as public needs an
 * authenticated principal, so a new route is protected until a rule opens it up.
 */
export function decide(method: string, path: string, principal: Principal | null): Decision {
  if (PUBLIC_PATHS.has(path)) {
    return 'allow';
  }
  if (!principal) {
    return 'unauthenticated';
  }
  if (ADMIN_ONLY.some((rule) => rule(method, path)) && !principal.roles.includes(ADMIN)) {
    return 'forbidden';
  }
  return 'allow';
}

export function isApiPath(path: string): boolean {
  return path === '/api' || path.startsWith('/api/');
}

/** Only same-site relative paths are accepted as post-login destinations (no open redirect). */
export function safeReturnTo(value: string | null | undefined): string {
  const sameSite = !!value && value.startsWith('/') && !value.startsWith('//') && !value.startsWith('/\\');
  return sameSite ? value : '/';
}
