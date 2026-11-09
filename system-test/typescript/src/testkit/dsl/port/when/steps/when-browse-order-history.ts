import type { ThenOrderHistoryResultStage } from '../../then/then-order-history-result-stage.js';

export interface WhenBrowseOrderHistory {
  withOrderNumber(orderNumber: string): this;
  withPage(page: string): this;
  withSize(size: string): this;
  /** Filters by the customer known in this scenario by the given alias, or by the default customer (customer1). */
  withCustomer(alias?: string): this;
  then(): ThenOrderHistoryResultStage;
}
