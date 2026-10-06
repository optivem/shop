import { type NextRequest, NextResponse } from 'next/server';
import * as client from 'openid-client';
import { oidcConfiguration, publicBaseUrl } from '@/lib/auth/oidc';
import { realmRoles } from '@/lib/auth/bearer';
import {
  LOGIN_COOKIE,
  SESSION_COOKIE,
  cookieOptions,
  openLoginState,
  sealSession,
  sessionCookieOptions,
} from '@/lib/auth/session';
import { unauthorizedResponse } from '@/lib/errors';

/** Completes the login: exchanges the code (verifying state, nonce and PKCE) and starts the session. */
export async function GET(request: NextRequest) {
  const base = publicBaseUrl(request.headers);
  const login = await openLoginState(request.cookies.get(LOGIN_COOKIE)?.value);
  if (!login) {
    return unauthorizedResponse('Login session expired; please try again');
  }

  try {
    const callbackUrl = new URL(`${request.nextUrl.pathname}${request.nextUrl.search}`, base);
    const tokens = await client.authorizationCodeGrant(oidcConfiguration(), callbackUrl, {
      pkceCodeVerifier: login.codeVerifier,
      expectedState: login.state,
      expectedNonce: login.nonce,
      idTokenExpected: true,
    });
    const claims = tokens.claims();
    if (!claims || !tokens.id_token) {
      return unauthorizedResponse('Login failed');
    }

    const name = typeof claims.preferred_username === 'string' ? claims.preferred_username : claims.sub;
    const response = NextResponse.redirect(new URL(login.returnTo, base));
    response.cookies.set(
      SESSION_COOKIE,
      await sealSession({
        subject: claims.sub,
        name,
        roles: realmRoles(claims.realm_access),
        idToken: tokens.id_token,
      }),
      sessionCookieOptions
    );
    response.cookies.set(LOGIN_COOKIE, '', cookieOptions(0));
    return response;
  } catch {
    return unauthorizedResponse('Login failed');
  }
}
