// Users of the test realm (docker/keycloak/shop-realm.json). Test credentials only.
export interface TestUser {
  readonly username: string;
  readonly password: string;
}

export const TestUsers = {
  CUSTOMER: { username: 'customer1', password: 'customer1-test-password' },
  ADMIN: { username: 'admin1', password: 'admin1-test-password' },
} as const satisfies Record<string, TestUser>;
