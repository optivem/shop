import type { ThenProfileResultStage } from '../../then/steps/then-profile.js';

export interface WhenViewProfile {
  /** A visitor who is asked to sign in signs in as the customer, and is then shown the profile (UI only). */
  withSignInWhenAsked(): this;
  then(): ThenProfileResultStage;
}
