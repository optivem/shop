// OIDC settings resolved at runtime from /config.js (container env vars), with local defaults.

export interface AuthConfig {
  keycloakUrl: string;
  realm: string;
  clientId: string;
}

interface RuntimeConfig {
  keycloakUrl?: string;
  keycloakRealm?: string;
  keycloakClientId?: string;
}

declare global {
  interface Window {
    __APP_CONFIG__?: RuntimeConfig;
  }
}

// An empty value counts as unset, so this is deliberately not `??`.
function valueOrDefault(value: string | undefined, fallback: string): string {
  return value !== undefined && value !== '' ? value : fallback;
}

/**
 * Resolves the auth settings, or undefined when authentication is disabled (no Keycloak URL configured).
 * The local Keycloak defaults apply only under the Vite dev server.
 */
export function getAuthConfig(
  runtime: RuntimeConfig | undefined = globalThis.window.__APP_CONFIG__,
  useDevDefaults: boolean = import.meta.env.DEV,
): AuthConfig | undefined {
  const keycloakUrl = valueOrDefault(runtime?.keycloakUrl, useDevDefaults ? 'http://localhost:8191' : '');
  if (keycloakUrl === '') return undefined;
  return {
    keycloakUrl: keycloakUrl.replace(/\/+$/, ''),
    realm: valueOrDefault(runtime?.keycloakRealm, 'shop'),
    clientId: valueOrDefault(runtime?.keycloakClientId, 'shop-frontend'),
  };
}

export function getAuthority(config: AuthConfig): string {
  return `${config.keycloakUrl}/realms/${config.realm}`;
}
