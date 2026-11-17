import type { ThenResultStage } from '../../then/then-result-stage.js';

export interface WhenViewOrder {
  withOrderNumber(orderNumber: string): this;
  /** A visitor who is asked to sign in signs in as the customer, and is then shown the order (UI only). */
  withSignInWhenAsked(): this;
  then(): ThenResultStage;
}
