import { createRemoteJWKSet, jwtVerify, type JWTVerifyGetKey } from 'jose';
import type { AuthConfig } from './auth.config';

export interface AuthenticatedUser {
  subject: string;
  username: string;
  roles: string[];
  /** The `email`, `given_name` and `family_name` claims; null when the token does not carry them. */
  email: string | null;
  givenName: string | null;
  familyName: string | null;
}

/**
 * Validates a bearer token: signature against the identity provider's JWKS, issuer, audience and
 * expiry (jose checks exp/nbf). All cryptography is done by the `jose` library.
 */
export class JwtVerifier {
  private readonly keys: JWTVerifyGetKey;

  constructor(
    private readonly config: AuthConfig,
    keys?: JWTVerifyGetKey,
  ) {
    this.keys = keys ?? createRemoteJWKSet(new URL(config.jwkSetUri));
  }

  /** Throws when the token is not valid; callers treat any throw as "not authenticated". */
  async verify(token: string): Promise<AuthenticatedUser> {
    const { payload } = await jwtVerify(token, this.keys, {
      issuer: this.config.issuer,
      audience: this.config.audience,
      algorithms: ['RS256'],
    });
    const subject = payload.sub ?? '';
    const preferred = payload.preferred_username;
    return {
      subject,
      username: typeof preferred === 'string' ? preferred : subject,
      roles: realmRoles(payload),
      email: stringClaim(payload.email),
      givenName: stringClaim(payload.given_name),
      familyName: stringClaim(payload.family_name),
    };
  }
}

function stringClaim(value: unknown): string | null {
  return typeof value === 'string' ? value : null;
}

function realmRoles(payload: Record<string, unknown>): string[] {
  const realmAccess = payload.realm_access;
  if (typeof realmAccess !== 'object' || realmAccess === null) return [];
  const roles = (realmAccess as { roles?: unknown }).roles;
  return Array.isArray(roles)
    ? roles.filter((r): r is string => typeof r === 'string')
    : [];
}
