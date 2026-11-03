import type { UserIdentity } from './user-identity.js';

/**
 * Selects who subsequent driver operations are performed as. Kept apart from MyShopDriver, which declares only
 * system operations (each taking a *Request and returning Result<*Response, …>): choosing the acting identity is
 * session state, not an operation on the system.
 */
export interface ActingUserSelector {
  /** Selects who subsequent operations are performed as. */
  actAs(identity: UserIdentity): void;
}
