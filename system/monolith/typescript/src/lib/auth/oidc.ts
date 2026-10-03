import * as client from 'openid-client';
import { authSettings } from './settings';

/**
 * Built from explicit endpoints (no discovery): the browser-facing and server-to-server URLs differ inside
 * Docker, so the issuer's discovery document cannot be fetched from the container.
 */
export function oidcConfiguration(): client.Configuration {
  const s = authSettings();
  const config = new client.Configuration(
    {
      issuer: s.issuer,
      authorization_endpoint: s.authorizationEndpoint,
      token_endpoint: s.tokenEndpoint,
      jwks_uri: s.jwksUri,
      end_session_endpoint: s.endSessionEndpoint,
    },
    s.clientId,
    s.clientSecret
  );
  // The local and pipeline stacks run Keycloak over plain HTTP.
  if (s.tokenEndpoint.startsWith('http://')) {
    client.allowInsecureRequests(config);
  }
  return config;
}

/** Public base URL of this app as the browser sees it (the Host header, since the container port is remapped). */
export function publicBaseUrl(headers: Headers): string {
  const proto = headers.get('x-forwarded-proto') ?? 'http';
  const host = headers.get('x-forwarded-host') ?? headers.get('host') ?? 'localhost';
  return `${proto}://${host}`;
}
