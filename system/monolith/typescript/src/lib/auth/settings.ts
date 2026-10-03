import { envOrDefault } from '@/lib/env';

export interface AuthSettings {
  /** Browser-facing issuer: what appears in tokens' iss and in the login redirect. */
  issuer: string;
  authorizationEndpoint: string;
  /** Server-to-server endpoints may differ from the browser-facing ones inside Docker. */
  tokenEndpoint: string;
  jwksUri: string;
  endSessionEndpoint: string;
  audience: string;
  clientId: string;
  clientSecret: string;
  sessionSecret: string;
}

export function authSettings(): AuthSettings {
  const issuer = envOrDefault('AUTH_ISSUER_URI', 'http://localhost:8391/realms/shop');
  const oidc = `${issuer}/protocol/openid-connect`;
  return {
    issuer,
    authorizationEndpoint: envOrDefault('AUTH_AUTHORIZATION_URI', `${oidc}/auth`),
    tokenEndpoint: envOrDefault('AUTH_TOKEN_URI', `${oidc}/token`),
    jwksUri: envOrDefault('AUTH_JWK_SET_URI', `${oidc}/certs`),
    endSessionEndpoint: envOrDefault('AUTH_END_SESSION_URI', `${oidc}/logout`),
    audience: envOrDefault('AUTH_AUDIENCE', 'shop-backend'),
    clientId: envOrDefault('AUTH_CLIENT_ID', 'shop-monolith'),
    // Clearly-marked test-realm values; supply real ones via the environment outside local/pipeline stacks.
    clientSecret: envOrDefault('AUTH_CLIENT_SECRET', 'shop-monolith-test-secret'),
    sessionSecret: envOrDefault('AUTH_SESSION_SECRET', 'shop-monolith-test-session-secret-change-me'),
  };
}
