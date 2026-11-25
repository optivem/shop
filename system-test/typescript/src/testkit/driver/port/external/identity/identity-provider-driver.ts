import type { UserAccount } from '../../user-account.js';

/**
 * The identity provider's test realm, for scenarios that need a user of their own: a customer (no saved profile, no
 * orders) or an admin. Unlike the system drivers it arranges, never asserts, so a failure is thrown rather than
 * returned. Closing it removes every account it created.
 */
export interface IdentityProviderDriver {
  /** Creates a new customer sign-in account with this name and email (a unique one when omitted), and the customer role. */
  createCustomer(firstName: string, lastName: string, email?: string): Promise<UserAccount>;
  /** Creates a new admin sign-in account with this name and email (a unique one when omitted), and the admin role. */
  createAdmin(firstName: string, lastName: string, email?: string): Promise<UserAccount>;
  close(): Promise<void>;
}
