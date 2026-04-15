import type { WhenStep } from './when-step.js';

export interface WhenCancelOrder extends WhenStep {
  withOrderNumber(orderNumber: string): WhenCancelOrder;
}
