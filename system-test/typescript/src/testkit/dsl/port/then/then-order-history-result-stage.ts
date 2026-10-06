import type { ThenFailure } from './steps/then-failure.js';
import type { ThenOrderHistory } from './steps/then-order-history.js';

export interface ThenOrderHistoryResultStage {
  shouldSucceed(): ThenOrderHistorySuccess;
  shouldFail(): ThenFailure;
}

export interface ThenOrderHistorySuccess extends PromiseLike<void> {
  and(): this;
  orderHistory(): ThenOrderHistory;
}
