import type { ConfigService } from '@nestjs/config';

export interface AuthConfig {
  issuer: string;
  jwkSetUri: string;
  audience: string;
}

// Required, no defaults: a backend that silently skipped token validation because a variable was
// missing would be open to everyone, so a missing value stops startup instead.
export function loadAuthConfig(config: ConfigService): AuthConfig {
  const read = (name: string): string => {
    const value = config.get<string>(name);
    if (!value) {
      throw new Error(
        `${name} must be set (JWT validation cannot start without it)`,
      );
    }
    return value;
  };
  return {
    issuer: read('AUTH_ISSUER_URI'),
    jwkSetUri: read('AUTH_JWK_SET_URI'),
    audience: read('AUTH_AUDIENCE'),
  };
}
