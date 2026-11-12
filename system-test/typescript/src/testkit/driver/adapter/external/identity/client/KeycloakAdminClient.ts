import { envOrDefault } from '../../../../../common/fallback.js';

export const REALM = 'shop';
const ADMIN_CLIENT_ID = 'admin-cli';
const DEFAULT_ADMIN = 'admin';
const LOCAL_HOSTS = new Set(['localhost', '127.0.0.1', '[::1]', '::1']);

/**
 * Keycloak's admin REST API, for the test realm of a local Keycloak only (the docker-compose stacks). It signs in
 * to the master realm as the bootstrap admin, which the stacks set to admin/admin (KC_BOOTSTRAP_ADMIN_*);
 * KEYCLOAK_ADMIN_USERNAME / KEYCLOAK_ADMIN_PASSWORD override that. A Keycloak that is not on this machine is
 * refused before any call, so the shared cloud Keycloak and its QA/production realms are never touched.
 */
export class KeycloakAdminClient {
  private readonly baseUrl: string;

  constructor(
    keycloakBaseUrl: string | undefined,
    private readonly adminUsername: string = envOrDefault('KEYCLOAK_ADMIN_USERNAME', DEFAULT_ADMIN),
    private readonly adminPassword: string = envOrDefault('KEYCLOAK_ADMIN_PASSWORD', DEFAULT_ADMIN),
  ) {
    this.baseUrl = requireLocal(keycloakBaseUrl);
  }

  /** Creates an enabled user with a permanent password and returns its id. */
  async createUser(username: string, password: string, firstName: string, lastName: string, email: string): Promise<string> {
    const response = await this.send('POST', '/users', {
      username,
      enabled: true,
      firstName,
      lastName,
      email,
      emailVerified: true,
      requiredActions: [],
      credentials: [{ type: 'password', value: password, temporary: false }],
    });
    await this.expectStatus(response, 201, `create user ${username}`);
    const location = response.headers.get('Location');
    if (!location) {
      throw new Error(`Keycloak created user ${username} in realm ${REALM} but sent no Location header`);
    }
    return location.substring(location.lastIndexOf('/') + 1);
  }

  /** Gives the user a realm role, e.g. CUSTOMER. */
  async assignRealmRole(userId: string, roleName: string): Promise<void> {
    const roleResponse = await this.send('GET', `/roles/${encodeURIComponent(roleName)}`);
    await this.expectStatus(roleResponse, 200, `read realm role ${roleName}`);
    const role: unknown = await roleResponse.json();
    const mapping = await this.send('POST', `/users/${encodeURIComponent(userId)}/role-mappings/realm`, [role]);
    await this.expectStatus(mapping, 204, `give user ${userId} the realm role ${roleName}`);
  }

  /** Deletes the user; one that is already gone counts as deleted. */
  async deleteUser(userId: string): Promise<void> {
    const response = await this.send('DELETE', `/users/${encodeURIComponent(userId)}`);
    if (response.status !== 404) {
      await this.expectStatus(response, 204, `delete user ${userId}`);
    }
  }

  private async send(method: string, path: string, body?: unknown): Promise<Response> {
    const url = `${this.baseUrl}/admin/realms/${REALM}${path}`;
    const headers: Record<string, string> = { Authorization: `Bearer ${await this.adminToken()}` };
    if (body !== undefined) headers['Content-Type'] = 'application/json';
    try {
      return await fetch(url, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) });
    } catch (e) {
      throw new Error(`Keycloak admin call ${method} ${url} failed: ${describeError(e)}`, { cause: e });
    }
  }

  /** A fresh master-realm admin token (they live for a minute, so one per call keeps it simple). */
  private async adminToken(): Promise<string> {
    const url = `${this.baseUrl}/realms/master/protocol/openid-connect/token`;
    let response: Response;
    try {
      response = await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: new URLSearchParams({
          grant_type: 'password',
          client_id: ADMIN_CLIENT_ID,
          username: this.adminUsername,
          password: this.adminPassword,
        }),
      });
    } catch (e) {
      throw new Error(`Keycloak admin sign-in at ${url} failed: ${describeError(e)}`, { cause: e });
    }
    if (response.status === 401) {
      throw new Error(
        `Keycloak at ${this.baseUrl} refused the admin sign-in as '${this.adminUsername}' (HTTP 401). The local stacks ` +
          'use admin/admin (KC_BOOTSTRAP_ADMIN_*); set KEYCLOAK_ADMIN_USERNAME and KEYCLOAK_ADMIN_PASSWORD if yours differ.',
      );
    }
    await this.expectStatus(response, 200, 'get an admin token');
    const json = (await response.json()) as { access_token?: string };
    if (!json.access_token) {
      throw new Error(`Keycloak at ${this.baseUrl} sent an admin token response without access_token`);
    }
    return json.access_token;
  }

  private async expectStatus(response: Response, expected: number, action: string): Promise<void> {
    if (response.status !== expected) {
      throw new Error(`Keycloak at ${this.baseUrl} could not ${action} in realm ${REALM}: HTTP ${response.status} ${await response.text()}`);
    }
  }
}

function requireLocal(keycloakBaseUrl: string | undefined): string {
  if (!keycloakBaseUrl) {
    throw new Error(
      "A new customer needs Keycloak: set KEYCLOAK_URL (or KEYCLOAK_URL_<MODE>) to the local stack's Keycloak, e.g. http://localhost:8392.",
    );
  }
  const host = new URL(keycloakBaseUrl).hostname.toLowerCase();
  if (!LOCAL_HOSTS.has(host)) {
    throw new Error(
      `A new customer is created only in a local Keycloak (localhost), never in a shared one; KEYCLOAK_URL is ${keycloakBaseUrl}. ` +
        'Run this scenario against a local stack.',
    );
  }
  return keycloakBaseUrl.replace(/\/+$/, '');
}

function describeError(e: unknown): string {
  if (!(e instanceof Error)) return JSON.stringify(e);
  return e.cause instanceof Error ? `${e.message} (${e.cause.message})` : e.message;
}
