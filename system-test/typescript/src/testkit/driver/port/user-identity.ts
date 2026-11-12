import type { CustomerAccount } from './customer-account.js';

/**
 * Who an operation is performed as. A shared test customer carries its zero-based position (0 = customer1,
 * 1 = customer2, ...); a customer created for the scenario carries its account instead (and position -1).
 */
export type UserIdentity =
  | { readonly kind: 'DEFAULT' }
  | { readonly kind: 'ANONYMOUS' }
  | { readonly kind: 'ADMIN' }
  | { readonly kind: 'CUSTOMER'; readonly customerIndex: number; readonly account?: CustomerAccount };

/** The number of shared test customers in the identity provider (customer1, customer2). */
const CUSTOMER_COUNT = 2;

export const UserIdentity = {
  CUSTOMER_COUNT,
  /** Customer for customer operations, admin for admin-only operations. */
  DEFAULT: { kind: 'DEFAULT' },
  /** No credentials at all. */
  ANONYMOUS: { kind: 'ANONYMOUS' },
  ADMIN: { kind: 'ADMIN' },
  /** The username of the shared test customer at the given zero-based position (0 = customer1). */
  customerUsername(customerIndex: number): string {
    return `customer${customerIndex + 1}`;
  },
  /**
   * The sign-in username of a customer identity (customer1, customer2, ..., or the created account's), as the
   * identity provider's test realm knows it; used where the system takes a customer by username.
   */
  usernameOf(identity: UserIdentity): string {
    if (identity.kind !== 'CUSTOMER') {
      throw new Error(`Only a customer identity has a customer username; this is ${identity.kind}`);
    }
    return identity.account?.username ?? `customer${identity.customerIndex + 1}`;
  },
  /** The shared test customer at the given zero-based position. */
  customer(customerIndex: number): UserIdentity {
    if (!Number.isInteger(customerIndex) || customerIndex < 0 || customerIndex >= CUSTOMER_COUNT) {
      throw new Error(`Test customer index ${customerIndex} is out of range; there are ${CUSTOMER_COUNT} test customers`);
    }
    return { kind: 'CUSTOMER', customerIndex };
  },
  /** A customer whose account was created for this scenario. */
  createdCustomer(account: CustomerAccount): UserIdentity {
    return { kind: 'CUSTOMER', customerIndex: -1, account };
  },
} as const satisfies Record<string, unknown>;
