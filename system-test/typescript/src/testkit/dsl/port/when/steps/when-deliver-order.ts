import type { ThenResultStage } from '../../then/then-result-stage.js';

export interface WhenDeliverOrder {
  withOrderNumber(orderNumber: string): this;
  then(): ThenResultStage;
}
