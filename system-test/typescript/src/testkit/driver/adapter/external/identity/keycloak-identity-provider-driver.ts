import { randomUUID } from 'node:crypto';
import type { UserAccount } from '../../../port/user-account.js';
import type { IdentityProviderDriver } from '../../../port/external/identity/identity-provider-driver.js';
import { KeycloakAdminClient } from './client/KeycloakAdminClient.js';

const CUSTOMER_ROLE = 'CUSTOMER';
const ADMIN_ROLE = 'ADMIN';
const CUSTOMER_USERNAME_PREFIX = 'new-customer-';
const ADMIN_USERNAME_PREFIX = 'new-admin-';

/**
 * Creates customers and admins in Keycloak's test realm, each with a unique username and a random password, and
 * deletes them again on close.
 */
export class KeycloakIdentityProviderDriver implements IdentityProviderDriver {
  private readonly client: KeycloakAdminClient;
  private readonly createdUserIds: string[] = [];

  constructor(keycloakBaseUrl: string | undefined) {
    this.client = new KeycloakAdminClient(keycloakBaseUrl);
  }

  async createCustomer(firstName: string, lastName: string, email?: string): Promise<UserAccount> {
    return this.createUser(CUSTOMER_USERNAME_PREFIX, CUSTOMER_ROLE, firstName, lastName, email);
  }

  async createAdmin(firstName: string, lastName: string, email?: string): Promise<UserAccount> {
    return this.createUser(ADMIN_USERNAME_PREFIX, ADMIN_ROLE, firstName, lastName, email);
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
