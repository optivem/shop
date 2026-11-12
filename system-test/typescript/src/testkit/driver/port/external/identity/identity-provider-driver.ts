import type { CustomerAccount } from '../../customer-account.js';

/**
 * The identity provider's test realm, for scenarios that need a customer of their own (no saved profile, no
 * orders). Unlike the system drivers it arranges, never asserts, so a failure is thrown rather than returned.
 * Closing it removes every account it created.
 */
export interface IdentityProviderDriver {
  /** Creates a new customer sign-in account with this name and email (a unique one when omitted), and the customer role. */
  createCustomer(firstName: string, lastName: string, email?: string): Promise<CustomerAccount>;
  close(): Promise<void>;
}
