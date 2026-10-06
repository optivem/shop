/**
 * Who an operation is performed as. A customer identity carries the zero-based position of the test customer
 * (0 = customer1, 1 = customer2, ...), so two identities are the same user exactly when they are equal.
 */
export type UserIdentity =
  | { readonly kind: 'DEFAULT' }
  | { readonly kind: 'ANONYMOUS' }
  | { readonly kind: 'ADMIN' }
  | { readonly kind: 'CUSTOMER'; readonly customerIndex: number };

/** The number of test customers available in the identity provider (customer1, customer2). */
const CUSTOMER_COUNT = 2;

export const UserIdentity = {
  CUSTOMER_COUNT,
  /** Customer for customer operations, admin for admin-only operations. */
  DEFAULT: { kind: 'DEFAULT' },
  /** No credentials at all. */
  ANONYMOUS: { kind: 'ANONYMOUS' },
  ADMIN: { kind: 'ADMIN' },
  /** The test customer at the given zero-based position. */
  customer(customerIndex: number): UserIdentity {
    if (!Number.isInteger(customerIndex) || customerIndex < 0 || customerIndex >= CUSTOMER_COUNT) {
      throw new Error(`Test customer index ${customerIndex} is out of range; there are ${CUSTOMER_COUNT} test customers`);
    }
    return { kind: 'CUSTOMER', customerIndex };
  },
} as const satisfies Record<string, unknown>;
