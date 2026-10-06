import { type NextRequest, NextResponse } from 'next/server';
import { verifyBearerToken } from '@/lib/auth/bearer';
import { decide, isApiPath, type Principal } from '@/lib/auth/policy';
import { SESSION_COOKIE, openSession } from '@/lib/auth/session';
import { forbiddenResponse, unauthorizedResponse } from '@/lib/errors';

const BEARER_PREFIX = 'bearer ';
const SAFE_METHODS = new Set(['GET', 'HEAD', 'OPTIONS']);

interface Resolved {
  principal: Principal | null;
  viaSession: boolean;
}

/** A presented Bearer token is judged on its own: an invalid one is never rescued by a session cookie. */
async function resolvePrincipal(request: NextRequest): Promise<Resolved> {
  const authorization = request.headers.get('authorization');
  if (authorization?.toLowerCase().startsWith(BEARER_PREFIX)) {
    const identity = await verifyBearerToken(authorization.slice(BEARER_PREFIX.length).trim());
    return { principal: identity ? { roles: identity.roles } : null, viaSession: false };
  }
  const session = await openSession(request.cookies.get(SESSION_COOKIE)?.value);
  return { principal: session ? { roles: session.roles } : null, viaSession: session !== null };
}

export async function middleware(request: NextRequest) {
  const { method, nextUrl } = request;
  const path = nextUrl.pathname;
  const { principal, viaSession } = await resolvePrincipal(request);

  const decision = decide(method, path, principal);
  if (decision === 'forbidden') {
    return forbiddenResponse();
  }
  if (decision === 'unauthenticated') {
    if (isApiPath(path)) {
      return unauthorizedResponse('Authentication is required');
    }
    const login = new URL('/login', nextUrl);
    login.searchParams.set('returnTo', `${path}${nextUrl.search}`);
    return NextResponse.redirect(login);
  }

  // Cookie sessions are ambient credentials, so state-changing API calls must carry a custom header that a
  // cross-site request cannot add without a CORS preflight. Bearer clients are exempt.
  if (viaSession && isApiPath(path) && !SAFE_METHODS.has(method) && !request.headers.has('x-requested-with')) {
    return forbiddenResponse();
  }
  return NextResponse.next();
}

export const config = {
  runtime: 'nodejs',
  matcher: ['/((?!_next/static|_next/image|favicon.ico).*)'],
};
