// The single place that decides who may call what. The AuthGuard consults this table for every
// request, so a new controller route is protected without anyone remembering to annotate it.

export type Access = 'public' | 'authenticated' | 'admin';

export const ADMIN_ROLE = 'ADMIN';

interface RoutePolicy {
  /** Upper-case HTTP method, or '*' for any. */
  method: string;
  /** Matched against the normalised (lower-case, no query, no trailing slash) path. */
  path: RegExp;
  access: Access;
}

// First match wins. Anything not listed falls through to DEFAULT_ACCESS (secure by default).
const POLICIES: readonly RoutePolicy[] = [
  { method: 'OPTIONS', path: /^\/.*$/, access: 'public' },
  { method: 'GET', path: /^\/health$/, access: 'public' },
  { method: 'POST', path: /^\/api\/orders\/[^/]+\/deliver$/, access: 'admin' },
  { method: 'POST', path: /^\/api\/coupons$/, access: 'admin' },
  { method: 'GET', path: /^\/api\/coupons$/, access: 'admin' },
  { method: '*', path: /^\/api\/admin(\/.*)?$/, access: 'admin' },
];

export const DEFAULT_ACCESS: Access = 'authenticated';

// Express routes case-insensitively and ignores a trailing slash, so the policy lookup has to
// see the same path the router will; otherwise "/API/ADMIN/..." would dodge an admin rule.
export function normalisePath(rawUrl: string): string {
  const withoutQuery = rawUrl.split(/[?#]/, 1)[0] ?? '';
  let path: string;
  try {
    path = decodeURIComponent(withoutQuery);
  } catch {
    path = withoutQuery;
  }
  path = path.toLowerCase().replace(/\/{2,}/g, '/');
  return path.length > 1 ? path.replace(/\/+$/, '') : path;
}

export function accessFor(method: string, rawUrl: string): Access {
  const m = method.toUpperCase();
  const path = normalisePath(rawUrl);
  const match = POLICIES.find(
    (p) => (p.method === '*' || p.method === m) && p.path.test(path),
  );
  return match?.access ?? DEFAULT_ACCESS;
}
