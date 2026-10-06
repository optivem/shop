// Users of the test realm (docker/keycloak/shop-realm.json). Test credentials only.
export interface TestUser {
  readonly username: string;
  readonly password: string;
}

export const TestUsers = {
  CUSTOMER: { username: 'customer1', password: 'customer1-test-password' },
  CUSTOMER2: { username: 'customer2', password: 'customer2-test-password' },
  ADMIN: { username: 'admin1', password: 'admin1-test-password' },
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
