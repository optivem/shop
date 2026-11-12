import { envOrDefault } from '../../../../../common/fallback.js';

export const REALM = 'shop';
const PRODUCTION_REALM = 'shop-prod';
const ADMIN_CLIENT_ID = 'admin-cli';
const DEFAULT_ADMIN = 'admin';
const LOCAL_HOSTS = new Set(['localhost', '127.0.0.1', '[::1]', '::1']);

/** How to get an admin token: the token endpoint, its form, and what to say when Keycloak refuses it. */
interface TokenGrant {
  tokenUrl: string;
  form: Record<string, string>;
  refusedMessage: string;
}

/**
 * Keycloak's admin REST API, for the test realm only. How it signs in depends on where Keycloak is:
 * - A local Keycloak (localhost, the docker-compose stacks): the master realm's bootstrap admin, which the stacks
 *   set to admin/admin (KC_BOOTSTRAP_ADMIN_*); KEYCLOAK_ADMIN_USERNAME / KEYCLOAK_ADMIN_PASSWORD override that.
 * - Any other Keycloak (the shared cloud one): a confidential service-account client of the test realm itself,
 *   holding only realm-management's user roles (client-credentials grant, KEYCLOAK_TEST_ADMIN_CLIENT_ID /
 *   KEYCLOAK_TEST_ADMIN_CLIENT_SECRET). KEYCLOAK_TEST_ADMIN_REALM must name the test realm, and the production realm
 *   is refused whatever the config says. Both checks run before any call, and the master admin is never used.
 */
export class KeycloakAdminClient {
  private readonly baseUrl: string;
  private readonly tokenGrant: TokenGrant;

  /** With adminUsername/adminPassword: signs in as that master-realm admin, a local Keycloak only. */
  constructor(keycloakBaseUrl: string | undefined, adminUsername?: string, adminPassword?: string) {
    this.baseUrl = requireUrl(keycloakBaseUrl);
    if (adminUsername !== undefined || adminPassword !== undefined) {
      if (!isLocal(this.baseUrl)) {
        throw new Error(
          `The master-realm admin is used only for a local Keycloak (localhost), never for a shared one; KEYCLOAK_URL is ${this.baseUrl}.`,
        );
      }
      this.tokenGrant = masterAdminGrant(this.baseUrl, adminUsername ?? DEFAULT_ADMIN, adminPassword ?? DEFAULT_ADMIN);
    } else {
      this.tokenGrant = isLocal(this.baseUrl)
        ? masterAdminGrant(
            this.baseUrl,
            envOrDefault('KEYCLOAK_ADMIN_USERNAME', DEFAULT_ADMIN),
            envOrDefault('KEYCLOAK_ADMIN_PASSWORD', DEFAULT_ADMIN),
          )
        : serviceAccountGrant(this.baseUrl);
    }
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

  /**
   * Gives the user a realm role, e.g. CUSTOMER. The role is looked up among the roles the user can be given, which
   * needs only view-users (reading the realm's roles directly needs view-realm, which the service account lacks).
   */
  async assignRealmRole(userId: string, roleName: string): Promise<void> {
    const available = await this.send('GET', `/users/${encodeURIComponent(userId)}/role-mappings/realm/available`);
    await this.expectStatus(available, 200, `list the realm roles user ${userId} can be given`);
    const roles = (await available.json()) as { name?: string }[];
    const role = roles.find((candidate) => candidate.name === roleName);
    if (!role) {
      throw new Error(
        `Keycloak at ${this.baseUrl} has no realm role ${roleName} that user ${userId} can be given in realm ${REALM}: ${JSON.stringify(roles)}`,
      );
    }
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

  /** A fresh admin token (they live for a minute, so one per call keeps it simple). */
  private async adminToken(): Promise<string> {
    const url = this.tokenGrant.tokenUrl;
    let response: Response;
    try {
      response = await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: new URLSearchParams(this.tokenGrant.form),
      });
    } catch (e) {
      throw new Error(`Keycloak admin sign-in at ${url} failed: ${describeError(e)}`, { cause: e });
    }
    if (response.status === 401) {
      throw new Error(this.tokenGrant.refusedMessage);
    }
    await this.expectStatus(response, 200, 'get an admin token');
    const json = (await response.json()) as { access_token?: string };
    if (!json.access_token) {
      throw new Error(`Keycloak at ${this.baseUrl} sent an admin token response without access_token`);
    }
    return json.access_token;
  }

  private async expectStatus(response: Response, expected: number, action: string): Promise<void> {
    if (response.status === 403) {
      throw new Error(
        `Keycloak at ${this.baseUrl} did not allow the admin account to ${action} in realm ${REALM} (HTTP 403). The service ` +
          "account needs realm-management's manage-users, view-users and query-users, which the acceptance stage's " +
          `deploy-keycloak job grants: ${await response.text()}`,
      );
    }
    if (response.status !== expected) {
      throw new Error(`Keycloak at ${this.baseUrl} could not ${action} in realm ${REALM}: HTTP ${response.status} ${await response.text()}`);
    }
  }
}

function masterAdminGrant(baseUrl: string, adminUsername: string, adminPassword: string): TokenGrant {
  return {
    tokenUrl: `${baseUrl}/realms/master/protocol/openid-connect/token`,
    form: { grant_type: 'password', client_id: ADMIN_CLIENT_ID, username: adminUsername, password: adminPassword },
    refusedMessage:
      `Keycloak at ${baseUrl} refused the admin sign-in as '${adminUsername}' (HTTP 401). The local stacks ` +
      'use admin/admin (KC_BOOTSTRAP_ADMIN_*); set KEYCLOAK_ADMIN_USERNAME and KEYCLOAK_ADMIN_PASSWORD if yours differ.',
  };
}

/**
 * The test realm's service account, for a shared Keycloak. Refuses the production realm, and any realm that
 * KEYCLOAK_TEST_ADMIN_REALM does not name, before anything is sent.
 */
function serviceAccountGrant(baseUrl: string): TokenGrant {
  const clientId = process.env.KEYCLOAK_TEST_ADMIN_CLIENT_ID ?? '';
  const clientSecret = process.env.KEYCLOAK_TEST_ADMIN_CLIENT_SECRET ?? '';
  const allowedRealm = process.env.KEYCLOAK_TEST_ADMIN_REALM ?? '';
  if (allowedRealm === PRODUCTION_REALM) {
    throw new Error(
      `Refusing to create users in the production realm ${PRODUCTION_REALM} of the Keycloak at ${baseUrl}: ` +
        `KEYCLOAK_TEST_ADMIN_REALM must name the test realm (${REALM}). Nothing was sent to Keycloak.`,
    );
  }
  const missing = ([
    ['KEYCLOAK_TEST_ADMIN_CLIENT_ID', clientId],
    ['KEYCLOAK_TEST_ADMIN_CLIENT_SECRET', clientSecret],
    ['KEYCLOAK_TEST_ADMIN_REALM', allowedRealm],
  ] as const)
    .filter(([, value]) => value.trim() === '')
    .map(([name]) => name);
  if (missing.length > 0) {
    throw new Error(
      `A new customer in the shared Keycloak at ${baseUrl} needs the test realm's service account, but ${missing.join(', ')} ` +
        'is not set. Set KEYCLOAK_TEST_ADMIN_CLIENT_ID (shop-system-test-admin), KEYCLOAK_TEST_ADMIN_CLIENT_SECRET and ' +
        `KEYCLOAK_TEST_ADMIN_REALM=${REALM} (the cloud acceptance stage sets them, the secret from the ` +
        'KEYCLOAK_TEST_ADMIN_CLIENT_SECRET GitHub secret), or run against a local stack.',
    );
  }
  if (allowedRealm !== REALM) {
    throw new Error(
      `KEYCLOAK_TEST_ADMIN_REALM is '${allowedRealm}', but new customers are created in realm ${REALM}, which it does not ` +
        `allow. Set KEYCLOAK_TEST_ADMIN_REALM=${REALM}. Nothing was sent to the Keycloak at ${baseUrl}.`,
    );
  }
  return {
    tokenUrl: `${baseUrl}/realms/${REALM}/protocol/openid-connect/token`,
    form: { grant_type: 'client_credentials', client_id: clientId, client_secret: clientSecret },
    refusedMessage:
      `Keycloak at ${baseUrl} refused the service account '${clientId}' of realm ${REALM} (HTTP 401). ` +
      "KEYCLOAK_TEST_ADMIN_CLIENT_SECRET must match the client's secret, which the acceptance stage's deploy-keycloak " +
      'job sets from the GitHub secret of the same name; check that secret and re-run the stage.',
  };
}

function requireUrl(keycloakBaseUrl: string | undefined): string {
  if (!keycloakBaseUrl) {
    throw new Error(
      "A new customer needs Keycloak: set KEYCLOAK_URL (or KEYCLOAK_URL_<MODE>) to the stack's Keycloak, e.g. http://localhost:8392.",
    );
  }
  let host = '';
  try {
    host = new URL(keycloakBaseUrl).hostname;
  } catch (e) {
    throw new Error(`KEYCLOAK_URL is not an absolute URL: ${keycloakBaseUrl}`, { cause: e });
  }
  if (!host) {
    throw new Error(`KEYCLOAK_URL has no host: ${keycloakBaseUrl}`);
  }
  return keycloakBaseUrl.replace(/\/+$/, '');
}

function isLocal(keycloakBaseUrl: string): boolean {
  return LOCAL_HOSTS.has(new URL(keycloakBaseUrl).hostname.toLowerCase());
}

function describeError(e: unknown): string {
  if (!(e instanceof Error)) return JSON.stringify(e);
  return e.cause instanceof Error ? `${e.message} (${e.cause.message})` : e.message;
}
