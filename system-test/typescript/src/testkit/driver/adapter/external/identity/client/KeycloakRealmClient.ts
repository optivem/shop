import { type Result, success, failure } from '../../../../../common/result.js';
import { REALM } from './KeycloakAdminClient.js';

const MAX_BODY_IN_MESSAGE = 300;
const TIMEOUT_MS = 30_000;

/**
 * The test realm's public endpoints, which need no sign-in. Separate from KeycloakAdminClient, so checking that
 * Keycloak is up never needs admin credentials.
 */
export class KeycloakRealmClient {
  private readonly baseUrl: string;

  constructor(keycloakBaseUrl: string) {
    this.baseUrl = keycloakBaseUrl.replace(/\/+$/, '');
  }

  /**
   * The realm's OpenID discovery document answers 200 once Keycloak has finished bootstrapping and serves the
   * realm; during the bootstrap it answers 503.
   */
  async checkHealth(): Promise<Result<void, string>> {
    const url = `${this.baseUrl}/realms/${REALM}/.well-known/openid-configuration`;
    let response: Response;
    try {
      response = await fetch(url, { signal: AbortSignal.timeout(TIMEOUT_MS) });
    } catch (e) {
      return failure(`Keycloak at ${url} could not be reached: ${e instanceof Error ? e.message : String(e)}`);
    }
    if (response.ok) return success(undefined);
    const hint = response.status === 503 ? ' (503: Keycloak is still bootstrapping)' : '';
    const body = (await response.text()).slice(0, MAX_BODY_IN_MESSAGE);
    return failure(`Keycloak realm ${REALM} at ${url} answered HTTP ${response.status}${hint}: ${body}`);
  }
}
