import type { Result } from '../../../../common/result.js';
import type { UserAccount } from '../../user-account.js';
import type { IdentityProviderErrorResponse } from './dtos/errors/IdentityProviderErrorResponse.js';

/**
 * The identity provider's test realm, for scenarios that need a user of their own: a customer (no saved profile, no
 * orders) or an admin. Apart from {@link goToIdentityProvider} it arranges, never asserts, so a failure is thrown
 * rather than returned. Closing it removes every account it created.
 */
export interface IdentityProviderDriver {
  /** Whether the test realm is served, without signing in; the smoke tests' check. */
  goToIdentityProvider(): Promise<Result<void, IdentityProviderErrorResponse>>;
  /** Creates a new customer sign-in account with this name and email (a unique one when omitted), and the customer role. */
  createCustomer(firstName: string, lastName: string, email?: string): Promise<UserAccount>;
  /** Creates a new admin sign-in account with this name and email (a unique one when omitted), and the admin role. */
  createAdmin(firstName: string, lastName: string, email?: string): Promise<UserAccount>;
  /**
   * A new unique username and password for a customer account that is created elsewhere (a visitor's sign-up,
   * UC-013); call {@link deleteOnClose} once it exists.
   */
  newAccountCredentials(): UserAccount;
  /** A unique email for an account from {@link newAccountCredentials}. */
  newAccountEmail(account: UserAccount): string;
  /** Removes the account with this username on close, like the ones this driver created. */
  deleteOnClose(username: string): Promise<void>;
  close(): Promise<void>;
}
