import type { ThenStep } from './then-step.js';

export interface ThenProduct extends ThenStep<ThenProduct> {
  hasSku(sku: string): ThenProduct;
  hasPrice(price: number): ThenProduct;
}
