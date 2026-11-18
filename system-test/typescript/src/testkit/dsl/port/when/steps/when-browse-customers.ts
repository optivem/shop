import type { ThenCustomerListResultStage } from '../../then/then-customer-list-result-stage.js';

/** Asks for the customers who have placed orders (admin only; API channel). */
export interface WhenBrowseCustomers {
  then(): ThenCustomerListResultStage;
}
