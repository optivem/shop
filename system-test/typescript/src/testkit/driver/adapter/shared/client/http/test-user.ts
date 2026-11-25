import type { UserIdentity } from '../../../../port/user-identity.js';

// Customers of the test realm (docker/keycloak/shop-realm.json), or a customer or admin created for the scenario.
// Test credentials only.
export interface TestUser {
  readonly username: string;
  readonly password: string;
  /** Whether this user has the admin role. */
  readonly admin?: boolean;
}

export const TestUsers = {
  CUSTOMER: { username: 'customer1', password: 'customer1-test-password' },
  CUSTOMER2: { username: 'customer2', password: 'customer2-test-password' },
} as const satisfies Record<string, TestUser>;

const CUSTOMERS: readonly TestUser[] = [TestUsers.CUSTOMER, TestUsers.CUSTOMER2];

/** The test customer at the given zero-based position (0 = customer1). */
export function testCustomer(index: number): TestUser {
  const customer = CUSTOMERS[index];
  if (!customer) {
    throw new Error(`No test customer at index ${index}`);
  }
  return customer;
}

/** The user behind a customer identity: a shared test customer, or the account created for the scenario. */
export function testUserFor(identity: Extract<UserIdentity, { kind: 'CUSTOMER' }>): TestUser {
  return identity.account ?? testCustomer(identity.customerIndex);
}

/** The user behind a customer or admin identity. */
export function testUserOf(identity: Extract<UserIdentity, { kind: 'CUSTOMER' | 'ADMIN' }>): TestUser {
  return identity.kind === 'ADMIN' ? { ...identity.account, admin: true } : testUserFor(identity);
}

/** Whether two test users are the same sign-in account. */
export function sameTestUser(a: TestUser, b: TestUser): boolean {
  return a.username === b.username;
}
