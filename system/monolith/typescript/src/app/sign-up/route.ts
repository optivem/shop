import { type NextRequest, NextResponse } from 'next/server';
import { publicBaseUrl } from '@/lib/auth/oidc';
import { safeReturnTo } from '@/lib/auth/policy';
import { SESSION_COOKIE, openSession } from '@/lib/auth/session';

/**
 * `/sign-up?returnUrl=…` (UC-013): the header's "Sign up" link. Like `/sign-in`, but the login it starts opens
 * Keycloak's registration form, so a visitor creates a customer account and comes back to `returnUrl` signed in;
 * a visitor who is already signed in goes straight there.
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
  login.searchParams.set('signUp', '1');
  return NextResponse.redirect(login);
}
