import type { TestUser } from './test-user.js';

const CLIENT_ID = 'shop-system-test';
const EXPIRY_MARGIN_MS = 30_000;
const DEFAULT_EXPIRES_IN_SECONDS = 60;

interface CachedToken {
  value: string;
  expiresAt: number;
}

/**
 * Acquires access tokens with the password grant (test realm only). Tokens are cached per
 * Keycloak instance and user for the lifetime of the process and refreshed shortly before expiry.
 */
export class KeycloakTokenProvider {
  private static readonly providers = new Map<string, KeycloakTokenProvider>();

  private readonly tokenUrl: string;
  private readonly cache = new Map<string, Promise<CachedToken>>();

  private constructor(keycloakBaseUrl: string) {
    this.tokenUrl = `${keycloakBaseUrl.replace(/\/+$/, '')}/realms/shop/protocol/openid-connect/token`;
  }

  static forBaseUrl(keycloakBaseUrl: string): KeycloakTokenProvider {
    let provider = KeycloakTokenProvider.providers.get(keycloakBaseUrl);
    if (!provider) {
      provider = new KeycloakTokenProvider(keycloakBaseUrl);
      KeycloakTokenProvider.providers.set(keycloakBaseUrl, provider);
    }
    return provider;
  }

  async getToken(user: TestUser): Promise<string> {
    const cached = this.cache.get(user.username);
    if (cached) {
      const token = await cached.catch(() => undefined);
      if (token && Date.now() < token.expiresAt) return token.value;
    }
    const pending = this.fetchToken(user);
    this.cache.set(user.username, pending);
    try {
      return (await pending).value;
    } catch (e) {
      this.cache.delete(user.username);
      throw e;
    }
  }

  private async fetchToken(user: TestUser): Promise<CachedToken> {
    const response = await fetch(this.tokenUrl, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({
        grant_type: 'password',
        client_id: CLIENT_ID,
        username: user.username,
        password: user.password,
      }),
    });
    if (!response.ok) {
      throw new Error(
        `Failed to acquire token for ${user.username} from ${this.tokenUrl}: HTTP ${response.status} ${await response.text()}`,
      );
    }
    const json = (await response.json()) as { access_token: string; expires_in?: number };
    const expiresInMs = (json.expires_in ?? DEFAULT_EXPIRES_IN_SECONDS) * 1000;
    return { value: json.access_token, expiresAt: Date.now() + expiresInMs - EXPIRY_MARGIN_MS };
  }
}

/** Authorization header for a test user, or no headers at all when Keycloak is not configured. */
export async function authHeadersFor(keycloakBaseUrl: string | undefined, user: TestUser): Promise<Record<string, string>> {
  if (!keycloakBaseUrl) return {};
  const token = await KeycloakTokenProvider.forBaseUrl(keycloakBaseUrl).getToken(user);
  return { Authorization: `Bearer ${token}` };
}
