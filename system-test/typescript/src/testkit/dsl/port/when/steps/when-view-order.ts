import type { WhenStep } from './when-step.js';

export interface WhenViewOrder extends WhenStep {
  withOrderNumber(orderNumber: string): WhenViewOrder;
}
