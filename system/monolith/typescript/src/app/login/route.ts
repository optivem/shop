import { type NextRequest, NextResponse } from 'next/server';
import * as client from 'openid-client';
import { oidcConfiguration, publicBaseUrl } from '@/lib/auth/oidc';
import { safeReturnTo } from '@/lib/auth/policy';
import { LOGIN_COOKIE, loginCookieOptions, sealLoginState } from '@/lib/auth/session';

/** Starts the authorization-code flow with PKCE (S256), state and nonce. */
export async function GET(request: NextRequest) {
  const base = publicBaseUrl(request.headers);
  const codeVerifier = client.randomPKCECodeVerifier();
  const state = client.randomState();
  const nonce = client.randomNonce();

  const authorizationUrl = client.buildAuthorizationUrl(oidcConfiguration(), {
    redirect_uri: `${base}/login/callback`,
    scope: 'openid profile email',
    code_challenge: await client.calculatePKCECodeChallenge(codeVerifier),
    code_challenge_method: 'S256',
    state,
    nonce,
  });

  const response = NextResponse.redirect(authorizationUrl);
  const returnTo = safeReturnTo(request.nextUrl.searchParams.get('returnTo'));
  response.cookies.set(LOGIN_COOKIE, await sealLoginState({ state, nonce, codeVerifier, returnTo }), loginCookieOptions);
  return response;
}
