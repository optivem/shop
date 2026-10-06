import type { ThenOrderHistoryResultStage } from '../../then/then-order-history-result-stage.js';

export interface WhenBrowseOrderHistory {
  withOrderNumber(orderNumber: string): this;
  then(): ThenOrderHistoryResultStage;
}
