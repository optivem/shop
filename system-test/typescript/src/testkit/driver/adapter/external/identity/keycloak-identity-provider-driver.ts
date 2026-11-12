import { randomUUID } from 'node:crypto';
import type { CustomerAccount } from '../../../port/customer-account.js';
import type { IdentityProviderDriver } from '../../../port/external/identity/identity-provider-driver.js';
import { KeycloakAdminClient } from './client/KeycloakAdminClient.js';

const CUSTOMER_ROLE = 'CUSTOMER';
const USERNAME_PREFIX = 'new-customer-';

/**
 * Creates customers in Keycloak's test realm, each with a unique username and a random password, and
 * deletes them again on close.
 */
export class KeycloakIdentityProviderDriver implements IdentityProviderDriver {
  private readonly client: KeycloakAdminClient;
  private readonly createdUserIds: string[] = [];

  constructor(keycloakBaseUrl: string | undefined) {
    this.client = new KeycloakAdminClient(keycloakBaseUrl);
  }

  async createCustomer(firstName: string, lastName: string, email?: string): Promise<CustomerAccount> {
    const username = `${USERNAME_PREFIX}${randomUUID()}`;
    const password = randomUUID();
    const userId = await this.client.createUser(username, password, firstName, lastName, email ?? `${username}@example.com`);
    this.createdUserIds.push(userId);
    await this.client.assignRealmRole(userId, CUSTOMER_ROLE);
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
      throw new Error(`Could not delete the customers created for this scenario: ${failures.join('; ')}`);
    }
  }
}
