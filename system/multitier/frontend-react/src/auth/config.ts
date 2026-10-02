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

export function getAuthConfig(runtime: RuntimeConfig | undefined = globalThis.window.__APP_CONFIG__): AuthConfig {
  return {
    keycloakUrl: valueOrDefault(runtime?.keycloakUrl, 'http://localhost:8191').replace(/\/+$/, ''),
    realm: valueOrDefault(runtime?.keycloakRealm, 'shop'),
    clientId: valueOrDefault(runtime?.keycloakClientId, 'shop-frontend'),
  };
}

export function getAuthority(config: AuthConfig): string {
  return `${config.keycloakUrl}/realms/${config.realm}`;
}
