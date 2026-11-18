import type { ThenFailure } from './steps/then-failure.js';
import type { ThenCustomerList } from './steps/then-customer-list.js';

export interface ThenCustomerListResultStage {
  shouldSucceed(): ThenCustomerListSuccess;
  shouldFail(): ThenFailure;
}

export interface ThenCustomerListSuccess extends PromiseLike<void> {
  and(): this;
  /** The customer list the executed browse returned. */
  customerList(): ThenCustomerList;
}
