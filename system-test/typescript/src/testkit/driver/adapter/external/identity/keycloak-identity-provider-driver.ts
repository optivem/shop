import { randomUUID } from 'node:crypto';
import { type Result, failure } from '../../../../common/result.js';
import type { UserAccount } from '../../../port/user-account.js';
import type { IdentityProviderDriver } from '../../../port/external/identity/identity-provider-driver.js';
import type { IdentityProviderErrorResponse } from '../../../port/external/identity/dtos/errors/IdentityProviderErrorResponse.js';
import { KeycloakAdminClient } from './client/KeycloakAdminClient.js';
import { KeycloakRealmClient } from './client/KeycloakRealmClient.js';

const CUSTOMER_ROLE = 'CUSTOMER';
const ADMIN_ROLE = 'ADMIN';
const CUSTOMER_USERNAME_PREFIX = 'new-customer-';
const ADMIN_USERNAME_PREFIX = 'new-admin-';

/**
 * Creates customers and admins in Keycloak's test realm, each with a unique username and a random password, and
 * deletes them again on close. The admin client signs in only when a user is first created, so
 * {@link goToIdentityProvider} needs no admin credentials.
 */
export class KeycloakIdentityProviderDriver implements IdentityProviderDriver {
  private adminClient?: KeycloakAdminClient;
  private readonly createdUserIds: string[] = [];

  constructor(private readonly keycloakBaseUrl: string | undefined) {}

  async goToIdentityProvider(): Promise<Result<void, IdentityProviderErrorResponse>> {
    if (!this.keycloakBaseUrl) {
      return failure({ message: 'Keycloak base URL is not configured: set KEYCLOAK_URL (or KEYCLOAK_URL_<MODE>)' });
    }
    const result = await new KeycloakRealmClient(this.keycloakBaseUrl).checkHealth();
    return result.success ? result : failure({ message: result.error });
  }

  private get client(): KeycloakAdminClient {
    this.adminClient ??= new KeycloakAdminClient(this.keycloakBaseUrl);
    return this.adminClient;
  }

  async createCustomer(firstName: string, lastName: string, email?: string): Promise<UserAccount> {
    return this.createUser(CUSTOMER_USERNAME_PREFIX, CUSTOMER_ROLE, firstName, lastName, email);
  }

  async createAdmin(firstName: string, lastName: string, email?: string): Promise<UserAccount> {
    return this.createUser(ADMIN_USERNAME_PREFIX, ADMIN_ROLE, firstName, lastName, email);
  }

  newAccountCredentials(): UserAccount {
    return { username: `${CUSTOMER_USERNAME_PREFIX}${randomUUID()}`, password: randomUUID() };
  }

  newAccountEmail(account: UserAccount): string {
    return `${account.username}@example.com`;
  }

  async deleteOnClose(username: string): Promise<void> {
    this.createdUserIds.push(await this.client.findUserId(username));
  }

  private async createUser(
    usernamePrefix: string,
    role: string,
    firstName: string,
    lastName: string,
    email?: string,
  ): Promise<UserAccount> {
    const username = `${usernamePrefix}${randomUUID()}`;
    const password = randomUUID();
    const userId = await this.client.createUser(username, password, firstName, lastName, email ?? `${username}@example.com`);
    this.createdUserIds.push(userId);
    await this.client.assignRealmRole(userId, role);
    if (role === ADMIN_ROLE) {
      // Every new user gets the realm's default role, which makes a visitor who signs up a customer (UC-013).
      // An admin is not a customer.
      await this.client.removeDefaultRoles(userId);
    }
    return { username, password };
  }

  async close(): Promise<void> {
    const failures: string[] = [];
    for (const userId of this.createdUserIds.splice(0)) {
      try {
        await this.client.deleteUser(userId);
      } catch (e) {
        failures.push(e instanceof Error ? e.message : JSON.stringify(e));
      }
    }
    if (failures.length > 0) {
      throw new Error(`Could not delete the users created for this scenario: ${failures.join('; ')}`);
    }
  }
}
