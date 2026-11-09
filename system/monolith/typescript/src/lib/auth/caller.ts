import type { NextRequest } from 'next/server';
import { ADMIN } from './policy';
import { verifyBearerToken } from './bearer';
import { SESSION_COOKIE, openSession } from './session';

const BEARER_PREFIX = 'bearer ';

/**
 * The authenticated caller of an API request: token subject, display username, whether they are an admin, and
 * the account's email and name claims (null when absent).
 */
export interface Caller {
  subject: string;
  username: string;
  admin: boolean;
  email: string | null;
  firstName: string | null;
  lastName: string | null;
}

/**
 * Resolves the caller the same way the middleware authenticated them: a presented Bearer token is judged on its
 * own, otherwise the session cookie is used. Null means unauthenticated (the middleware has already rejected
 * those, so handlers only see null if it is misconfigured).
 */
export async function resolveCaller(request: NextRequest): Promise<Caller | null> {
  const authorization = request.headers.get('authorization');
  if (authorization?.toLowerCase().startsWith(BEARER_PREFIX)) {
    const identity = await verifyBearerToken(authorization.slice(BEARER_PREFIX.length).trim());
    return identity
      ? {
        subject: identity.subject,
        username: identity.username,
        admin: identity.roles.includes(ADMIN),
        email: identity.email,
        firstName: identity.givenName,
        lastName: identity.familyName,
      }
      : null;
  }
  const session = await openSession(request.cookies.get(SESSION_COOKIE)?.value);
  return session
    ? {
      subject: session.subject,
      username: session.name,
      admin: session.roles.includes(ADMIN),
      email: session.email,
      firstName: session.givenName,
      lastName: session.familyName,
    }
    : null;
}
