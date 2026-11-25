import type { GivenStage } from '../given-stage.js';
import type { WhenStage } from '../../when/when-stage.js';
import type { ThenStage } from '../../then/then-stage.js';

/**
 * A customer of the scenario's own: a new sign-in account with no saved profile and no orders. It becomes the
 * scenario's customer, so `loggedInAsCustomer()`, `profile().savedByCustomer()` and `order().placedByCustomer()`
 * all mean this customer. Unset names keep the defaults (Ana Novak); the email is unique unless set.
 */
export interface GivenNewCustomer {
  /** The sign-in account's first name. */
  withFirstName(firstName: string): GivenNewCustomer;
  /** The sign-in account's last name. */
  withLastName(lastName: string): GivenNewCustomer;
  /** The sign-in account's email. */
  withEmail(email: string): GivenNewCustomer;
  /** The shipping country chosen on the sign-up form; only with {@link signedUpThroughTheShop}. */
  withCountry(country: string): GivenNewCustomer;
  /**
   * The visitor creates the account themselves (UC-013): the shop's "Sign up" link and the identity provider's
   * registration form, instead of the identity provider's admin API. UI channel only.
   */
  signedUpThroughTheShop(): GivenNewCustomer;
  and(): GivenStage;
  when(): WhenStage;
  then(): ThenStage;
}
