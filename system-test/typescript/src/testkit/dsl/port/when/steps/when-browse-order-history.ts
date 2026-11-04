import type { ThenOrderHistoryResultStage } from '../../then/then-order-history-result-stage.js';

export interface WhenBrowseOrderHistory {
  withOrderNumber(orderNumber: string): this;
  withPage(page: string): this;
  withSize(size: string): this;
  then(): ThenOrderHistoryResultStage;
}
