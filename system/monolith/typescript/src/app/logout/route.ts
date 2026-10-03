import { type NextRequest, NextResponse } from 'next/server';
import * as client from 'openid-client';
import { oidcConfiguration, publicBaseUrl } from '@/lib/auth/oidc';
import { authSettings } from '@/lib/auth/settings';
import { SESSION_COOKIE, cookieOptions, openSession } from '@/lib/auth/session';

/** Ends the local session and the Keycloak session, then returns to the home page (which re-prompts for login). */
export async function POST(request: NextRequest) {
  const base = publicBaseUrl(request.headers);
  const session = await openSession(request.cookies.get(SESSION_COOKIE)?.value);

  const params: Record<string, string> = {
    client_id: authSettings().clientId,
    post_logout_redirect_uri: `${base}/`,
  };
  if (session) {
    params.id_token_hint = session.idToken;
  }
  const endSession = client.buildEndSessionUrl(oidcConfiguration(), params);

  // 303 turns the POST into a GET on the Keycloak logout endpoint.
  const response = NextResponse.redirect(endSession, 303);
  response.cookies.set(SESSION_COOKIE, '', cookieOptions(0));
  return response;
}
