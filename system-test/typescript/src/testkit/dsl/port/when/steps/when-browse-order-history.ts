import type { ThenOrderHistoryResultStage } from '../../then/then-order-history-result-stage.js';

export interface WhenBrowseOrderHistory {
  withOrderNumber(orderNumber: string): this;
  /** Filters by part of the number of the order known by this alias, in lower case, so it matches that order only. */
  withOrderNumberPartOf(orderNumber: string): this;
  /** No order-number filter: every order the user may see (the default filters by the default order). */
  withNoOrderNumberFilter(): this;
  /** After browsing, sets the customer filter to All customers and clears the order number (UI only). */
  withFiltersResetAfterwards(): this;
  withPage(page: string): this;
  withSize(size: string): this;
  /** Filters by the customer known in this scenario by the given alias, or by the default customer (customer1). */
  withCustomer(alias?: string): this;
  then(): ThenOrderHistoryResultStage;
}
