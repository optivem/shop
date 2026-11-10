import { type NextRequest, NextResponse } from 'next/server';
import { publicBaseUrl } from '@/lib/auth/oidc';
import { safeReturnTo } from '@/lib/auth/policy';
import { SESSION_COOKIE, openSession } from '@/lib/auth/session';

/**
 * `/sign-in?returnUrl=…` (storefront-ux §6.4): the header's "Sign in" link. Starts the login and comes back to
 * `returnUrl` (default `/`); a visitor who is already signed in goes straight there.
 */
export async function GET(request: NextRequest) {
  const base = publicBaseUrl(request.headers);
  const returnUrl = safeReturnTo(request.nextUrl.searchParams.get('returnUrl'));
  const session = await openSession(request.cookies.get(SESSION_COOKIE)?.value);
  if (session) {
    return NextResponse.redirect(new URL(returnUrl, base));
  }
  const login = new URL('/login', base);
  login.searchParams.set('returnTo', returnUrl);
  return NextResponse.redirect(login);
}
