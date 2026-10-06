import { createRemoteJWKSet, jwtVerify } from 'jose';
import { authSettings } from './settings';

let jwks: ReturnType<typeof createRemoteJWKSet> | undefined;

function keySet() {
  jwks ??= createRemoteJWKSet(new URL(authSettings().jwksUri));
  return jwks;
}

export interface TokenIdentity {
  subject: string;
  username: string;
  roles: string[];
}

/** Validates signature (JWKS), issuer, audience and expiry; returns the caller's identity, or null if invalid. */
export async function verifyBearerToken(token: string): Promise<TokenIdentity | null> {
  const { issuer, audience } = authSettings();
  try {
    const { payload } = await jwtVerify(token, keySet(), { issuer, audience });
    const subject = payload.sub ?? '';
    const preferred = payload.preferred_username;
    return {
      subject,
      username: typeof preferred === 'string' ? preferred : subject,
      roles: realmRoles(payload.realm_access),
    };
  } catch {
    return null;
  }
}

export function realmRoles(realmAccess: unknown): string[] {
  if (typeof realmAccess !== 'object' || realmAccess === null) {
    return [];
  }
  const roles = (realmAccess as { roles?: unknown }).roles;
  return Array.isArray(roles) ? roles.filter((r): r is string => typeof r === 'string') : [];
}
